package com.github.j5ik2o.ak.es

import akka.NotUsed
import akka.stream.Attributes
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

  sealed trait FlowControlCommand
  case object First                 extends FlowControlCommand
  case class Next(scrollId: String) extends FlowControlCommand
  case object Complete              extends FlowControlCommand

  def searchRequestSource(
      searchRequest: SearchRequest,
      scroll: Scroll
  ): Source[SearchResponse, NotUsed] = {
    Source.unfold[FlowControlCommand, SearchResponse](First) {
      case Complete =>
        None
      case First =>
        val searchResponse = restHighLevelClient.search(searchRequest.scroll(scroll), options)
        val searchHits     = searchResponse.getHits.getHits
        if (searchHits != null && searchHits.nonEmpty)
          Some((Next(searchResponse.getScrollId), searchResponse))
        else
          Some((Complete, searchResponse))

      case Next(scrollId) =>
        val searchScrollRequest = new SearchScrollRequest(scrollId).scroll(scroll)
        val nextResponse        = restHighLevelClient.scroll(searchScrollRequest, options)
        val searchHits          = nextResponse.getHits.getHits
        if (searchHits != null && searchHits.nonEmpty)
          Some((Next(nextResponse.getScrollId), nextResponse))
        else
          Some((Complete, nextResponse))

    }
  }
}
