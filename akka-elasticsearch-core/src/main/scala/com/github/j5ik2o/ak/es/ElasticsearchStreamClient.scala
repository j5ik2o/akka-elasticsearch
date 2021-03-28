package com.github.j5ik2o.ak.es

import akka.NotUsed
import akka.stream.scaladsl.{ Concat, Flow, Source }
import org.elasticsearch.action.{ DocWriteRequest, DocWriteResponse }
import org.elasticsearch.action.bulk.{ BulkItemResponse, BulkRequest, BulkResponse }
import org.elasticsearch.action.index.{ IndexRequest, IndexResponse }
import org.elasticsearch.action.search.{ SearchRequest, SearchResponse, SearchScrollRequest }
import org.elasticsearch.client.{ RequestOptions, RestHighLevelClient }
import org.elasticsearch.search.Scroll

import scala.annotation.tailrec
import scala.jdk.CollectionConverters._

case class ElasticsearchStreamClient(
    restHighLevelClient: RestHighLevelClient,
    options: RequestOptions = RequestOptions.DEFAULT
) {

  def indexRequestFlow: Flow[IndexRequest, IndexResponse, NotUsed] = {
    Flow[IndexRequest].map { indexRequest =>
      restHighLevelClient.index(indexRequest, options)
    }
  }

  def bulkRequestFlow[T](batch: Int): Flow[DocWriteRequest[T], BulkResponse, NotUsed] = {
    Flow[DocWriteRequest[T]]
      .grouped(batch)
      .map { requests =>
        val bulkRequest = new BulkRequest()
        requests.foreach(request => bulkRequest.add(request))
        restHighLevelClient.bulk(bulkRequest, options)
      }
  }

  def toBulkItemResponseFlow: Flow[BulkResponse, Either[BulkItemResponse.Failure, DocWriteResponse], NotUsed] = {
    Flow[BulkResponse]
      .flatMapConcat { bulkResponse =>
        Source.fromIterator(() => bulkResponse.iterator().asScala)
      }.map { bulkItemResponse =>
        if (bulkItemResponse.isFailed) {
          Left(bulkItemResponse.getFailure)
        } else {
          Right(bulkItemResponse.getResponse.asInstanceOf[DocWriteResponse])
        }
      }
  }

  def searchRequestSource(
      searchRequest: SearchRequest,
      scroll: Scroll
  ): Source[SearchResponse, NotUsed] = {
    @tailrec
    def scrollLoop(
        scrollId: String,
        acc: Source[SearchResponse, NotUsed]
    ): Source[SearchResponse, NotUsed] = {
      val searchScrollRequest = new SearchScrollRequest(scrollId).scroll(scroll)
      val nextResponse        = restHighLevelClient.scroll(searchScrollRequest, options)
      val searchHits          = nextResponse.getHits.getHits
      if (searchHits != null && searchHits.nonEmpty) {
        val result = Source.combine(acc, Source.single(nextResponse))(Concat(_))
        scrollLoop(scrollId, result)
      } else {
        acc
      }
    }
    Source
      .single(searchRequest).map { request =>
        restHighLevelClient.search(request.scroll(scroll), options)
      }.flatMapConcat { searchResponse =>
        val searchHits = searchResponse.getHits.getHits
        if (searchHits != null && searchHits.nonEmpty) {
          scrollLoop(searchResponse.getScrollId, Source.single(searchResponse))
        } else {
          Source.single(searchResponse)
        }
      }
  }
}
