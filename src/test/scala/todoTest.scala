import java.time.LocalDateTime
import org.scalatest._
import org.scalatest.matchers.should.Matchers
import org.scalatest.funsuite.AnyFunSuite

class todoTest extends AnyFunSuite with Matchers {

  test("User authentication should register a new user, login, and logout") {
    val username = "testuser"
    val email = "testuser@example.com"
    val password = "testpassword"

    // Register a new user
    val registerResponse = MyApp.register(username, email, password)
    assert(registerResponse("status").str == "success")
    assert(registerResponse("username").str == username)
    val userId = registerResponse("userId").num.toInt
    val token = registerResponse("token").str
    val tokenExpiration = LocalDateTime.parse(registerResponse("tokenExpiration").str)

    // Login the user
    val loginResponse = MyApp.login(username, email, password)
    assert(loginResponse("status").str == "success")
    assert(loginResponse("token").str == token)
    assert(loginResponse("userId").num.toInt == userId)
    assert(loginResponse("username").str == username)




    // Logout the user
    val logoutResponse = MyApp.logout(token)
    assert(logoutResponse("status").str == "success")
  }
  
  
  
}

