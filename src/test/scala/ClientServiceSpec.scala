// Scala dependencies
import org.scalatest._
import skinny.http._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
// Java dependencies
import java.io.File
import com.doradosystems.integration.http.HttpUtils
import org.apache.commons.io.FileUtils
import org.apache.http.entity.ContentType;

class ClientServiceSpec extends FlatSpec with Matchers{

  final val ENDPOINT = "http://localhost:8081/client-service/v1"
  implicit val formats = DefaultFormats

  "GET/version" should "return a 200 status code and the correct version name" in {
    val response = HTTP.get(s"$ENDPOINT/version")
    response.status should equal (200)
    response.asString should equal ("client-service-1.0.2-SNAPSHOT")
  }

  "POST/customer" should "return a 200 status code and a Customer JSON in the response" in {
    val json = FileUtils.readFileToString(new File("src/test/resources/bar-customer.json"))
    val response = HttpUtils.post(s"$ENDPOINT/customer", json, ContentType.APPLICATION_JSON)
    response.getStatusLine.getStatusCode should equal (200)

    val customerJson = HttpUtils.getResponsePayloadAsString(response)
    val customer = parse(customerJson)
    (customer \ "customerId").extract[String] should not be empty
    (customer \ "name").extract[String] should equal ("bar")
    (customer \ "active").extract[Boolean] should equal (true)
    (customer \ "createdBy").extract[String] should equal ("test")
    (customer \ "updatedBy").extract[String] should equal ("test")
  }

  it should "return a 400 status code when the customer name already exists" in {
    val json = FileUtils.readFileToString(new File("src/test/resources/bar-customer.json"))
    val response = HttpUtils.post(s"$ENDPOINT/customer", json, ContentType.APPLICATION_JSON)
    response.getStatusLine.getStatusCode should equal (400)
    val error = parse(HttpUtils.getResponsePayloadAsString(response))
    (error \ "exception").extract[String] should equal ("com.doradosystems.exception.UniqueConstraintException")
    (error \ "message").extract[String] should equal ("PreparedStatementCallback; SQL []; ERROR: duplicate key value violates unique constraint \"customer_name_key\"\n  Detail: Key (name)=(bar) already exists.; nested exception is org.postgresql.util.PSQLException: ERROR: duplicate key value violates unique constraint \"customer_name_key\"\n  Detail: Key (name)=(bar) already exists.")
  }

  it should "return a 400 status code when the JSON request is invalid" in {
    val response = HttpUtils.post(s"$ENDPOINT/customer", "foo", ContentType.APPLICATION_JSON)
    response.getStatusLine.getStatusCode should equal (400)
    val error = parse(HttpUtils.getResponsePayloadAsString(response))
    (error \ "message").extract[String] should equal ("Bad Request")
  }

  it should "return a 400 status code when the Customer name is missing" in {
    val json = FileUtils.readFileToString(new File("src/test/resources/missing-customer-name.json"))
    val response = HttpUtils.post(s"$ENDPOINT/customer", json, ContentType.APPLICATION_JSON)
    response.getStatusLine.getStatusCode should equal (400)
    val error = parse(HttpUtils.getResponsePayloadAsString(response))
    (error \ "message").extract[String] should equal ("Bad Request")
  }

  "GET/customer" should "return a 200 status code and the existing customer in the response" in {
    val response = HTTP.get(s"$ENDPOINT/customer")
    response.status should equal (200)

    val customer = parse(response.asString)
    (customer \ "customerId").extract[String] should not be empty
    (customer \ "name").extract[String] should equal ("bar")
    (customer \ "active").extract[Boolean] should equal (true)
    (customer \ "createdBy").extract[String] should equal ("test")
    (customer \ "updatedBy").extract[String] should equal ("test")
  }

  it should "return a 200 status code and all existing customers in the response" in {
    val json = FileUtils.readFileToString(new File("src/test/resources/foo-customer.json"))
    val postResponse = HttpUtils.post(s"$ENDPOINT/customer", json, ContentType.APPLICATION_JSON)
    postResponse.getStatusLine.getStatusCode should equal (200)

    val getResponse = HTTP.get(s"$ENDPOINT/customer")
    getResponse.status should equal (200)

    info("GET/customer response: " + getResponse.asString)
    val customers = parse(getResponse.asString)
    customers.asInstanceOf[JArray].arr should have length (2)

    val barCustomer = customers(0).asInstanceOf[JObject]
    (barCustomer \ "customerId").extract[String] should not be empty
    (barCustomer \ "name").extract[String] should equal ("bar")
    (barCustomer \ "active").extract[Boolean] should equal (true)
    (barCustomer \ "createdAt").extract[Integer] should not be (null)
    (barCustomer \ "updatedAt").extract[Integer] should not be (null)
    (barCustomer \ "createdBy").extract[String] should equal ("test")
    (barCustomer \ "updatedBy").extract[String] should equal ("test")
    (barCustomer \ "contacts").extract[String] should be (null)

    val fooCustomer = customers(1).asInstanceOf[JObject]
    (fooCustomer \ "customerId").extract[String] should not be empty
    (fooCustomer \ "name").extract[String] should equal ("foo")
    (fooCustomer \ "active").extract[Boolean] should equal (true)
    (fooCustomer \ "createdAt").extract[Integer] should not be (null)
    (fooCustomer \ "updatedAt").extract[Integer] should not be (null)
    (fooCustomer \ "createdBy").extract[String] should equal ("fox")
    (fooCustomer \ "updatedBy").extract[String] should equal ("fox")
    (fooCustomer \ "contacts").extract[String] should be (null)
  }

  "GET/customer/id" should "return a 200 status code and the correct customer in the response" in {
    val json = FileUtils.readFileToString(new File("src/test/resources/fubar-customer.json"))
    val postResponse = HttpUtils.post(s"$ENDPOINT/customer", json, ContentType.APPLICATION_JSON)
    postResponse.getStatusLine.getStatusCode should equal (200)

    val customerJson = HttpUtils.getResponsePayloadAsString(postResponse)
    var customer = parse(customerJson)
    val customerId = (customer \ "customerId").extract[String]
    customerId should not be empty

    val getResponse = HTTP.get(s"$ENDPOINT/customer/$customerId")
    getResponse.status should equal (200)

    info("GET/customer/id response: " + getResponse.asString)
    customer = parse(getResponse.asString)
    (customer \ "customerId").extract[String] should equal (customerId)
    (customer \ "name").extract[String] should equal ("fubar")
    (customer \ "active").extract[Boolean] should equal (true)
    (customer \ "createdBy").extract[String] should equal ("fu")
    (customer \ "updatedBy").extract[String] should equal ("fu")
  }

  it should "return a 404 status code when the customer id is invalid" in {
    val response = HTTP.get(s"$ENDPOINT/customer/fcc03b38-b2a6-11e7-abc4-cec278b6b50a")
    response.status should equal (404)
    val error = parse(response.asString)
    (error \ "exception").extract[String] should equal ("com.doradosystems.exception.NotFoundException")
    (error \ "message").extract[String] should equal ("Customer fcc03b38-b2a6-11e7-abc4-cec278b6b50a not found.")
  }
}
