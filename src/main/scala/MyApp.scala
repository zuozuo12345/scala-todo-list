/**
 * We are developing a simple web app, completely in Scala. Importantly, the app is a Single Page App (SPA),
 * and interacts completely asynchronously with the back-end.
 * Its objective is to show simple UI element manipulation, as well as
 * interaction with a database.
 *
 * The app implements a simple TODO list.
 *
 * This app will be the basis for your project as well.
 */
import Utils.{checkPassword, generateToken, hashPassword, isTokenValid}
import cask.Request
import ujson.*

import java.sql.Connection
import java.sql.DriverManager
import scala.util.Try
import com.typesafe.config.ConfigFactory
import com.typesafe.config.{Config, ConfigFactory}
import org.mindrot.jbcrypt.BCrypt

import java.io.InputStreamReader
import java.lang.System.console
import java.sql.{Connection, DriverManager}
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.{Base64, Date}
import scala.collection.mutable
/** Cask is a web framework similar to Python's Flask. As a framework, it has its own 'main' and does a lot of magic
 *  behind the scenes. */

// User case class with additional fields for token and tokenExpiration
case class User(id: Int, username: String, email: String, hashedPassword: String, token: String, tokenExpiration: LocalDateTime)


object AppConfig {
  private val config = ConfigFactory.load()
  val secretKey: String = config.getString("app.secretKey")
  val conn = DbDetails.conn

}

object Utils {

  def hashPassword(password: String): String = BCrypt.hashpw(password, BCrypt.gensalt())

  def checkPassword(password: String, hashedPassword: String): Boolean = BCrypt.checkpw(password, hashedPassword)

  def generateToken(): String = {
    val random = new SecureRandom()
    val bytes = new Array[Byte](48)
    random.nextBytes(bytes)
    Base64.getUrlEncoder.withoutPadding().encodeToString(bytes)
  }

  def isTokenValid(user: User): Boolean = LocalDateTime.now().isBefore(user.tokenExpiration)
}

object Users {
  private val users = mutable.Map[Int, User]()
  private var nextId = 1

  def updateId(id: Int): Unit = {
    this.nextId = id
  }

  def addUser(user: User): Int = {
    val id = nextId
    nextId += 1
    users.put(id, user.copy(id = id))
    id
  }

  def getUser(id: Int): Option[User] = users.get(id)

  def addAllUser: Option[User] = {
    // Use the query to retrieve the user data from the database
    val stmt = DbDetails.conn.prepareStatement(""" SELECT "id", username, email, hashedPassword, token, tokenExpiration FROM users """)
    val rs = stmt.executeQuery()
    // If a row is returned, create a User object and return Some(user)
    if (rs.next()) {
      val id = rs.getInt("id")
      val username = rs.getString("username")
      val email = rs.getString("email")
      val hashedPassword = rs.getString("hashedPassword")
      val token = rs.getString("token")
      val tokenExpiration = rs.getTimestamp("tokenExpiration").toLocalDateTime()
      Some(User(id, username, email, hashedPassword, token, tokenExpiration))
    } else {
      None
    }
  }



  def getUserByEmail(email: String): Option[User] = {
    // Use the query to retrieve the user data from the database
    val stmt = DbDetails.conn.prepareStatement(""" SELECT "id", username, email, hashedPassword, token, tokenExpiration FROM users WHERE email = ? """)
    stmt.setString(1, email)
    val rs = stmt.executeQuery()

    // If a row is returned, create a User object and return Some(user)
    if (rs.next()) {
      val id = rs.getInt("id")
      val username = rs.getString("username")
      val hashedPassword = rs.getString("hashedPassword")
      val token = rs.getString("token")
      val tokenExpiration = rs.getTimestamp("tokenExpiration").toLocalDateTime()
      Some(User(id, username, email, hashedPassword, token, tokenExpiration))
    } else {
      None
    }
  }

  def getUserByToken(token: String): Option[User] =
    // Use the query to retrieve the user data from the database
    val stmt = DbDetails.conn.prepareStatement(""" SELECT "id", username, email, hashedPassword, token, tokenExpiration FROM users WHERE token = ? """)
    stmt.setString(1, token)
    val rs = stmt.executeQuery()
    // If a row is returned, create a User object and return Some(user)
    if (rs.next()) {
      val id = rs.getInt("id")
      val username = rs.getString("username")
      val email = rs.getString("email")
      val hashedPassword = rs.getString("hashedPassword")
      val token = rs.getString("token")
      val tokenExpiration = rs.getTimestamp("tokenExpiration").toLocalDateTime()
      Some(User(id, username, email, hashedPassword, token, tokenExpiration))
    } else {
      None
    }


  def updateUser(user: User): Unit = {users(user.id) = user}
}


object MyApp extends cask.MainRoutes:
  /** Allows access to the web server from a different machine */
  override def host: String = "0.0.0.0"
  /** Good to set the port explicitly, and not use port 80 for experiments -- leave encryption to a wrapper such as nginx */
  override def port: Int = sys.env.get("PORT").map(_.toInt).getOrElse(8000)
  /** Turn this on during development */
  override def debugMode: Boolean = true

  /** Homepage entry point */
  @cask.get("/")
  def index() = cask.Redirect("/static/index.html")

  /** Static file repository, in case we need to serve static files, e.g. index.html */
  @cask.staticFiles("/static/", headers = Seq("Content-Type" -> "text/html"))
  def staticFileRoutes1() = "/static/"

  /** Static file repository for Javascript files (the target compilation folder for our ScalaJS project) */
  @cask.staticFiles("/js/", headers = Seq("Content-Type" -> "text/javascript"))
  def staticFileRoutes2() = "/sjs/target/scala-3.2.2/sjs-fastopt/"


  val createusersStmt =  DbDetails.conn.prepareStatement("""CREATE TABLE IF NOT EXISTS "users" (
                                                  "id" SERIAL PRIMARY KEY,
                                                  username VARCHAR(50) NOT NULL,
                                                  email VARCHAR(255) NOT NULL UNIQUE,
                                                  hashedPassword VARCHAR(255) NOT NULL,
                                                  token VARCHAR(255) DEFAULT NULL,
                                                  tokenExpiration TIMESTAMP DEFAULT NULL
                                                  )""")
  createusersStmt.execute()
  createusersStmt.close()
  val createtodosStmt =  DbDetails.conn.prepareStatement("""CREATE TABLE IF NOT EXISTS todo (
                                                                 "id" SERIAL PRIMARY KEY ,
                                                                 item VARCHAR(255) NOT NULL,
                                                                 priority VARCHAR(255),
                                                                 deadline VARCHAR(255),
                                                                 finished BOOLEAN NOT NULL DEFAULT false,
                                                                 overdue BOOLEAN NOT NULL DEFAULT false,
                                                                 urgent BOOLEAN NOT NULL DEFAULT false,
                                                                 userId int NOT NULL
                                                                )""")
  createtodosStmt.execute()
  createtodosStmt.close()

  val insertStmt =  DbDetails.conn.prepareStatement("""INSERT INTO todo (item, priority, deadline,userId) VALUES ('test', '1', '2023-04-30','1');
                                                              INSERT INTO todo (item, priority, deadline,userId) VALUES ('Buy groceries', '1', '2023-04-01','1');
                                                              INSERT INTO todo (item, priority, deadline,userId) VALUES ('test2', '1', '2023-04-14','1');
                                                              INSERT INTO todo (item, priority, deadline,userId) VALUES ('Finish Tasks', '3', '2023-07-01','1');
                                                              INSERT INTO todo (item, priority, deadline,userId) VALUES ('Buy clothes', '3', '2023-05-01','1');
                                                              INSERT INTO todo (item, priority, deadline,userId) VALUES ('Study cooking', '2', '2023-07-01','1');
                                                              INSERT INTO todo (item, priority, deadline,userId) VALUES ( 'Test', '2', '2023-04-24','2');
                                                              INSERT INTO todo (item, priority, deadline,userId) VALUES ('test5', '2', '2023-04-30','2');
                                                              INSERT INTO todo (item, priority, deadline,userId) VALUES ( 'test6', '2', '2023-03-31','2');
                                                              INSERT INTO todo (item, priority, deadline,userId) VALUES ('test3', '1', '2023-04-17','3');
                                                              INSERT INTO todo (item, priority, deadline,userId) VALUES ( 'Eat cake', '2', '2023-04-29','2');
                                                              """)
  insertStmt.execute()
  insertStmt.close()



// New endpoint for user registration
  @cask.postJson("/register")
  def register(username: String, email: String, password: String): ujson.Obj = {
    Users.getUserByEmail(email) match {
      case Some(_) =>
        ujson.Obj("status" -> "error", "message" -> "Username already exists")
      case None =>
        val hashedPassword = Utils.hashPassword(password)
        val token = Utils.generateToken()
        val tokenExpiration = LocalDateTime.now().plus(24, ChronoUnit.HOURS)
        val user = User(0, username, email, hashedPassword, token, tokenExpiration)
        val userInsertStmt = DbDetails.conn.prepareStatement("""insert into users (username, email, hashedPassword, token, tokenExpiration) values(?, ?, ?, ?, ?)""")
        userInsertStmt.setString(1, username)
        userInsertStmt.setString(2, email)
        userInsertStmt.setString(3, hashedPassword)
        userInsertStmt.setString(4, token)
        userInsertStmt.setTimestamp(5, java.sql.Timestamp.valueOf(tokenExpiration))
        userInsertStmt.executeUpdate()
        val userSelectStmt = DbDetails.conn.prepareStatement("""SELECT "id" FROM users WHERE username = ? AND email = ?""")
        userSelectStmt.setString(1, username) // Assuming 'username' is a variable with the desired value
        userSelectStmt.setString(2, email) // Assuming 'email' is a variable with the desired value
        val qResults = userSelectStmt.executeQuery()

        if (qResults.next()) {
          val id = qResults.getInt("id") // Retrieve the 'id' from the ResultSet
          Users.updateId(id)
          Users.addUser(user)
          ujson.Obj("status" -> "success", "userId" -> id, "tokenExpiration" -> tokenExpiration.toString, "token" -> token, "username" -> username)
        } else {
          ujson.Obj("status" -> "error", "message" -> "get id error")
        }
    }
  }

  def authenticateUser(username: String, email: String, password: String): Option[User] = {
    Users.getUserByEmail(email).flatMap { user =>
      if (user.username == username && Utils.checkPassword(password, user.hashedPassword)) {
        val tokenExpiration = LocalDateTime.now().plus(24, ChronoUnit.HOURS)
        val updatedUser = user.copy( tokenExpiration = tokenExpiration)
        Users.updateUser(updatedUser)
        val updateStmt = DbDetails.conn.prepareStatement("""UPDATE users
                                              SET  tokenExpiration = ?
                                              WHERE id = ?""")
        updateStmt.setTimestamp(1, java.sql.Timestamp.valueOf(tokenExpiration))
        updateStmt.setInt(2, user.id)
        updateStmt.executeUpdate()
        Some(updatedUser)
      } else {
        None
      }
    }
  }

  @cask.postJson("/login")
  def login(username: String, email: String, password: String): ujson.Obj = {
    authenticateUser(username, email, password) match {
      case Some(user) =>
        ujson.Obj("status" -> "success", "token" -> user.token, "tokenExpiration" -> user.tokenExpiration.toString,"userId" -> user.id, "username"-> username)
      case None =>
        ujson.Obj("status" -> "error", "message" -> "Authentication failed")
    }
  }


// New endpoint for user logout
  @cask.postJson("/logout")
  def logout(token: String): ujson.Obj = {
    Users.getUserByToken(token) match {
      case Some(user) if Utils.isTokenValid(user) =>
        val updatedUser = user.copy(token = "", tokenExpiration = LocalDateTime.MIN)
        Users.addUser(updatedUser)
        ujson.Obj("status" -> "success")
      case _ =>
        ujson.Obj("status" -> "error", "message" -> "Invalid or expired token")
    }
  }



  /** End-point for reading all TODOs, for updating the item panel after every modification */

  @cask.get("/readtodos")
  def readtodos(request: Request): String = {
    val authorizationHeader = request.headers.get("authorization")
    println(s"Authorization header: $authorizationHeader")
    val tokenOpt = authorizationHeader.flatMap { header =>
      header.lastOption
    }
    tokenOpt match {
      case Some(token) =>
      Users.getUserByToken(token) match {
      case Some(user) if isTokenValid(user) =>
        val userId = user.id
        // Prepare the UPDATE statement
        val updateStmt = DbDetails.conn.prepareStatement("""UPDATE todo SET overdue = CASE WHEN CURRENT_DATE <= CAST(deadline AS DATE) THEN false ELSE true END WHERE userid = ? """)
        updateStmt.setInt(1, userId)
        // Execute the UPDATE statement
        updateStmt.executeUpdate()
        val todoupdateStmt = DbDetails.conn.prepareStatement("""UPDATE todo SET urgent = CASE WHEN (CAST(deadline AS DATE) - CURRENT_DATE) BETWEEN 0 AND 3 THEN true ELSE false END WHERE userid = ?""")
        todoupdateStmt.setInt(1, userId)
        // Execute the UPDATE statement
        todoupdateStmt.executeUpdate()
        // Execute the SELECT statement
        val selectStmt = DbDetails.conn.prepareStatement("""SELECT "id", item, priority, deadline, finished, overdue, urgent FROM todo WHERE userid = ? ORDER BY overdue ASC,finished ASC, priority ASC, deadline ASC""")
        selectStmt.setInt(1, userId)
        val qResults = selectStmt.executeQuery()
        // Process the query results
        val todos = Iterator.continually {
          if qResults.next() then
            Some(ujson.Obj(
              "id" -> qResults.getString("id"),
              "item" -> qResults.getString("item"),
              "priority" -> qResults.getString("priority"),
              "deadline" -> qResults.getString("deadline"),
              "finished" -> qResults.getBoolean("finished"),
              "overdue" -> qResults.getBoolean("overdue"),
              "urgent" -> qResults.getBoolean("urgent"),
            ))
          else None
        }.takeWhile(_.nonEmpty).flatten
  
        // Generate the JSON response
        val result =
          if todos.isEmpty
          then "[]"
          else todos.map(_.render()).mkString("[", ",", "]")
        val wrapped = s"""{ "items" : $result }"""
        ujson.Obj("status" -> "success", "message" -> "already check userid").render()
        println(wrapped)
        wrapped
      case _ =>
        ujson.Obj("status" -> "error", "message" -> "Invalid or expired token").render()
      }
      case None =>
        ujson.Obj("status" -> "error", "message" -> "Missing or invalid Authorization header").render()
    }
  }



  /** End-point for searching TODO items */
  @cask.post("/search")
  def search(request: Request): String = {
    val authorizationHeader = request.headers.get("authorization")
    println(s"Authorization header: $authorizationHeader")
    val tokenOpt = authorizationHeader.flatMap { header =>
      header.lastOption
    }
    tokenOpt match {
      case Some(token) =>
        Users.getUserByToken(token) match {
          case Some(user) if isTokenValid(user) =>
            val userId = user.id
            val searchText = request.text().toLowerCase
            val searchStmt = DbDetails.conn.prepareStatement(
              """SELECT item, priority, deadline, finished, overdue, urgent FROM todo WHERE userid = ? and LOWER(item) LIKE ? OR LOWER(CAST(priority AS CHAR)) LIKE ?""")
            searchStmt.setInt(1, userId)
            searchStmt.setString(2, s"%$searchText%")
            searchStmt.setString(3, s"%$searchText%")
            val qResults = searchStmt.executeQuery()
            val todos = Iterator.continually {
              if qResults.next() then
                Some(ujson.Obj(
                  "item" -> qResults.getString("item"),
                  "priority" -> qResults.getString("priority"),
                  "deadline" -> qResults.getString("deadline"),
                  "finished" -> qResults.getBoolean("finished"),
                  "overdue" -> qResults.getBoolean("overdue"),
                  "urgent" -> qResults.getBoolean("urgent"),
                ))
              else None
            }.takeWhile(_.nonEmpty).flatten
            val result =
              if todos.isEmpty
              then "[]"
              else todos.map(_.render()).mkString("[", ",", "]")
            val wrapped = s"""{ "items" : $result }"""
            wrapped
          case _ =>
            ujson.Obj("status" -> "error", "message" -> "Invalid or expired token").render()
          }
      case None =>
        ujson.Obj("status" -> "error", "message" -> "Missing or invalid Authorization header").render()
    }
  }


  /** End-point for marking a TODO item as finished or unfinished */
  @cask.put("/finish")
  def finish(request: cask.Request): Unit =
    val authorizationHeader = request.headers.get("authorization")
    println(s"Authorization header: $authorizationHeader")
    val tokenOpt = authorizationHeader.flatMap { header =>
      header.lastOption
    }
    tokenOpt match {
      case Some(token) =>
        Users.getUserByToken(token) match {
          case Some(user) if isTokenValid(user) =>
          val finishData = ujson.read(request.text())
          val itemid = finishData("id").str.toInt
          val finished = finishData("finished").bool
          val overdue = finishData("overdue").bool
          val finishStmt = DbDetails.conn.prepareStatement("""update todo set finished = ?,overdue = ? where id = ?""")
          finishStmt.setBoolean(1, finished)
          finishStmt.setBoolean(2, overdue)
          finishStmt.setInt(3, itemid)
          finishStmt.execute()
          case _ =>
            ujson.Obj("status" -> "error", "message" -> "Invalid or expired token").render()
        }
      case None =>
        ujson.Obj("status" -> "error", "message" -> "Missing or invalid Authorization header").render()
    }




  /** End-point for deleting one TODO item */
  @cask.delete("/delete")
  def delete(request: Request): Unit =
    val authorizationHeader = request.headers.get("authorization")
    println(s"Authorization header: $authorizationHeader")
    val tokenOpt = authorizationHeader.flatMap { header =>
      header.lastOption
    }
    tokenOpt match {
      case Some(token) =>
        Users.getUserByToken(token) match {
          case Some(user) if isTokenValid(user) =>
            val userId = user.id
            val delStmt = DbDetails.conn.prepareStatement("""delete from todo where id = ? and userid = ?""")
            val deleteData = ujson.read(request.text())
            val itemid = deleteData("id").str.toInt
            delStmt.setInt(1, itemid)
            delStmt.setInt(2, userId)
            delStmt.execute()
          case _ =>
            ujson.Obj("status" -> "error", "message" -> "Invalid or expired token").render()
        }
      case None =>
        ujson.Obj("status" -> "error", "message" -> "Missing or invalid Authorization header").render()
    }

  /** End-point for deleting one TODO item */
  @cask.post("/redo")
  def redo(request: Request): Unit =
    val authorizationHeader = request.headers.get("authorization")
    println(s"Authorization header: $authorizationHeader")
    val tokenOpt = authorizationHeader.flatMap { header =>
      header.lastOption
    }
    tokenOpt match {
      case Some(token) =>
        Users.getUserByToken(token) match {
          case Some(user) if isTokenValid(user) =>
            val userId = user.id
            val redoData = ujson.read(request.text())
            val item = redoData("item").str
            val priority = redoData("priority").str
            val deadline1 = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val deadline = deadline1.format(formatter)
            val redoStmt = DbDetails.conn.prepareStatement("""insert into todo (item, priority, deadline, finished, overdue, urgent,  userId)values(?, ?, ?, ?, ?, ?, ?)""")
            redoStmt.setString(1, item)
            redoStmt.setString(2, priority)
            redoStmt.setString(3, deadline)
            redoStmt.setBoolean(4, false)
            redoStmt.setBoolean(5, false)
            redoStmt.setBoolean(6, false)
            redoStmt.setInt(7, userId)
            redoStmt.execute()
          case _ =>
            ujson.Obj("status" -> "error", "message" -> "Invalid or expired token").render()
        }
      case None =>
        ujson.Obj("status" -> "error", "message" -> "Missing or invalid Authorization header").render()
    }


  @cask.post("/submit")
  def submit(request: Request): Unit = {
    val authorizationHeader = request.headers.get("authorization")
    println(s"Authorization header in submit: $authorizationHeader")
    val tokenOpt = authorizationHeader.flatMap { header =>
      header.lastOption
    }
    tokenOpt match {
      case Some(token) =>
        Users.getUserByToken(token) match {
          case Some(user) if isTokenValid(user) =>
            val userId = user.id
            // Parse the request body as JSON
            val requestBody = ujson.read(request.text())
            val item = requestBody("item").str
            val priority = requestBody("priority").str
            val deadline = requestBody("deadline").str
            val todoInsertStmt = DbDetails.conn.prepareStatement("""insert into todo (item, priority, deadline, finished, overdue, urgent,  userId) values(?, ?, ?, ?, ?, ?, ?)""")
            todoInsertStmt.setString(1, item)
            todoInsertStmt.setString(2, priority)
            todoInsertStmt.setString(3, deadline)
            todoInsertStmt.setBoolean(4, false)
            todoInsertStmt.setBoolean(5, false)
            todoInsertStmt.setBoolean(6, false)
            todoInsertStmt.setInt(7, userId)
            todoInsertStmt.execute()
            ujson.Obj("status" -> "success").render()
          case _ =>
            ujson.Obj("status" -> "error", "message" -> "Invalid or expired token").render()
        }
      case None =>
        ujson.Obj("status" -> "error", "message" -> "Missing or invalid Authorization header").render()
    }
  }


  @cask.put("/upPriority")
  def upPriority(request: cask.Request): Unit =
    val authorizationHeader = request.headers.get("authorization")
    println(s"Authorization header: $authorizationHeader")
    val tokenOpt = authorizationHeader.flatMap { header =>
      header.lastOption
    }
    tokenOpt match {
      case Some(token) =>
        Users.getUserByToken(token) match {
          case Some(user) if isTokenValid(user) =>
            val upPriorityData = ujson.read(request.text())
            val itemid = upPriorityData("id").str.toInt
            val priorityText = upPriorityData("priority").str.toInt
            val upStmt = DbDetails.conn.prepareStatement("""update todo set priority = ? where id = ?""")
            upStmt.setInt(1, priorityText)
            upStmt.setInt(2, itemid)
            upStmt.execute()
          case _ =>
            ujson.Obj("status" -> "error", "message" -> "Invalid or expired token").render()
        }
      case None =>
        ujson.Obj("status" -> "error", "message" -> "Missing or invalid Authorization header").render()
    }


  @cask.put("/downPriority")
  def downPriority(request: cask.Request): Unit =
    val authorizationHeader = request.headers.get("authorization")
    println(s"Authorization header for down: $authorizationHeader")
    val tokenOpt = authorizationHeader.flatMap { header =>
      header.lastOption
    }
    tokenOpt match {
      case Some(token) =>
        Users.getUserByToken(token) match {
          case Some(user) if isTokenValid(user) =>
            val downPriorityData = ujson.read(request.text())
            val itemid = downPriorityData("id").str.toInt
            val priorityText = downPriorityData("priority").str.toInt
            val downStmt = DbDetails.conn.prepareStatement("""update todo set priority = ? where id = ?""")
            downStmt.setInt(1, priorityText)
            downStmt.setInt(2, itemid)
            downStmt.execute()
          case _ =>
            ujson.Obj("status" -> "error", "message" -> "Invalid or expired token").render()
        }
      case None =>
        ujson.Obj("status" -> "error", "message" -> "Missing or invalid Authorization header").render()
    }

  initialize();
