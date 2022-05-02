package com.github.j5ik2o.ak.es

import akka.actor.ActorSystem
import akka.stream.ActorAttributes
import akka.stream.scaladsl.Source
import akka.testkit.TestKit
import org.apache.http.HttpHost
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.client.{ RequestOptions, RestClient, RestHighLevelClient }
import org.elasticsearch.core.TimeValue
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.Scroll
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.scalatest.{ BeforeAndAfter, BeforeAndAfterAll }
import org.scalatest.concurrent.{ Eventually, ScalaFutures }
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Millis, Seconds, Span }
import org.testcontainers.DockerClientFactory
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.utility.DockerImageName

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters._

class ElasticsearchStreamClientSpec
    extends TestKit(ActorSystem("ElasticsearchStreamClientSpec"))
    with AnyFreeSpecLike
    with Matchers
    with ScalaFutures
    with Eventually
    with BeforeAndAfter
    with BeforeAndAfterAll {

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(60, Seconds), interval = Span(500, Millis))

  val container = new ElasticsearchContainer(
    DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch").withTag("7.11.2")
  )

  before {
    container.start()
  }

  after {
    container.stop()
  }

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "ElasticsearchStreamClient" - {
    "bulkIndex" in {
      val client = new RestHighLevelClient(
        RestClient.builder(
          new HttpHost(DockerClientFactory.instance().dockerHostIpAddress(), container.getMappedPort(9200), "http"),
          new HttpHost(DockerClientFactory.instance().dockerHostIpAddress(), container.getMappedPort(9300), "http")
        )
      )

      val indexRequests = (for (i <- 1 to 500) yield {
        new IndexRequest("my_index").source(Map("message" -> s"$i, Hello, Elasticsearch").asJava)
      }).toVector

      val elasticsearchStreamClient = ElasticsearchStreamClient(client)
      val f = Source(indexRequests)
        .via(elasticsearchStreamClient.bulkRequestFlow(64)).via(elasticsearchStreamClient.toBulkItemResponseFlow)
        .withAttributes(ActorAttributes.dispatcher("blocking-io-dispatcher"))
        .runForeach { response =>
          println(s"response = $response")
        }

      Await.result(f, Duration.Inf)
    }
    "search" in {
      val client = new RestHighLevelClient(
        RestClient.builder(
          new HttpHost(DockerClientFactory.instance().dockerHostIpAddress(), container.getMappedPort(9200), "http"),
          new HttpHost(DockerClientFactory.instance().dockerHostIpAddress(), container.getMappedPort(9300), "http")
        )
      )
      for (i <- 1 to 500) {
        val indexRequest  = new IndexRequest("my_index").source(Map("message" -> s"$i, Hello, Elasticsearch").asJava)
        val indexResponse = client.index(indexRequest, RequestOptions.DEFAULT)
        println(s"indexResponse: ${indexResponse.getId}")
      }

      Thread.sleep(1000 * 3)

      val searchSourceBuilder = SearchSourceBuilder
        .searchSource()
        .query(QueryBuilders.matchQuery("message", "Elasticsearch"))

      val elasticsearchStreamClient = ElasticsearchStreamClient(client)
      val future =
        elasticsearchStreamClient
          .searchRequestSource(
            new SearchRequest("my_index").source(searchSourceBuilder),
            new Scroll(TimeValue.timeValueMinutes(1))
          )
          .withAttributes(ActorAttributes.dispatcher("blocking-io-dispatcher"))
          .zipWithIndex.runForeach { case (response, index) =>
            println(s"$index: response = $response")
          }
      Await.result(future, Duration.Inf)

    }
  }

}
