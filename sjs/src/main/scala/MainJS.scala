import MainJS.{createButton, createREText, createROText, createRWText, loginAction, logoutAction, registerAction, token, tokenExpiration, updatePanel, username}
import org.scalajs.dom.*
import org.scalajs.dom.html.Button
import org.scalajs.dom.experimental.*
import org.scalajs.dom.window.localStorage

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import ujson.*

import scala.scalajs.js.Thenable.Implicits.*
import scala.scalajs.js.annotation.*
import scala.util.{Failure, Success, Try}
import scala.scalajs.js.annotation.JSExportTopLevel
import org.scalajs.dom.*
import org.scalajs.dom.html.*
import org.scalajs.dom.window.alert

import scala.Option
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.scalajs.js
import scala.scalajs.js.*
import scala.scalajs.js.Thenable.Implicits.*
import scala.concurrent.ExecutionContext.Implicits.global

object MainJS:

//  var token: Option[String] = None // Add this line
  var token: String = "" // Add this line
  var tokenExpiration: Option[js.Date] = getTokenExpiration()
  var userId: Int = 0 // Add this line
  var username: String = ""

  def getTokenExpiration(): Option[js.Date] = {
    val storedTokenExpiration = localStorage.getItem("tokenExpiration")

    if (storedTokenExpiration != null) {
      val strippedTokenExpiration = storedTokenExpiration.stripPrefix("Some(").stripSuffix(")")
      Try(new js.Date(strippedTokenExpiration)).toOption
    } else {
      None
    }
  }

  @JSExportTopLevel("main")
  def main() =
    val workspace: Div = document.getElementById("workspace").asInstanceOf[Div]
    val loginArea = createLoginArea(registerAction, loginAction, logoutAction)
    workspace.appendChild(loginArea)
    workspace.appendChild(createInputArea())
    workspace.appendChild(createSearchArea())
    workspace.appendChild(createFilterArea())
    val panel = document.createElement("div").asInstanceOf[Div]                 // create the panel containing the todo items
    panel.id = "panel"                                                          // name it so it can be accessed later
    panel.style.background = "lightyellow"                                      // give it a distinguishing color so that it is easily identifiable visually
    workspace.appendChild(panel)                                                // add the panel to the workspace, below the input area
    updatePanel(contentAction(_, "", ""))()                                                             // populate the panel with the current todo items

  /** Create buttons with a suggestive animation when clicked on */
  def createButton(text: String, action: UIEvent => Unit): Button =
    val button = document.createElement("button").asInstanceOf[Button]
    button.innerText = text
    button.addEventListener("click", action)
    button
  
  /** Create read-only text fields */
  def createROText(text: String): Div =
    val div = document.createElement("div").asInstanceOf[Div]
    div.className = "divreadonly"
    div.style.display = "inline-block"  // stack them left-to-right instead of top-down
    div.innerText = text
    div
  
  /** Create read-write fields (i.e. input fields) */
  def createRWText(initialText: String): Div =
    val div = document.createElement("div").asInstanceOf[Div]
    div.className = "divreadwrite"
    div.style.display = "inline-block"     // stack them left-to-right instead of top-down
    div.contentEditable = "true"           // the attribute making fields editable.
    div.innerText = initialText
    div

  def createRLText(text: String): Label = {
    val label = document.createElement("label").asInstanceOf[Label]
    label.textContent = text
    label.style.display = "none"
    label
  }

  def createRAText(placeholder: String): Input = {
    val input = document.createElement("input").asInstanceOf[Input]
    input.setAttribute("type", "text")
    input.setAttribute("placeholder", placeholder)
    input.style.display = "inline-block"
    input.style.minWidth = "300px"
    input
  }



  def createREText(initialText: String): Div = {
    val div = document.createElement("div").asInstanceOf[Div]
    div.className = "divreadwrite"
    div.style.display = "inline-block" // stack them left-to-right instead of top-down
    div.contentEditable = "true" // the attribute making fields editable.
    div.innerText = initialText

    // Add onfocus event listener to clear initial text when the div is focused
    div.onfocus = (_: FocusEvent) => {
      if (div.innerText == initialText) {
        div.innerText = ""
      }
    }

    // Add onblur event listener to restore the initial text when the div is not focused, and it is empty
    div.onblur = (_: FocusEvent) => {
      if (div.innerText.trim.isEmpty) {
        div.innerText = initialText
      }
    }
    div
  }


  /** Create the input area, containing the editable field allowing the user to add todo items.
   * Adding items is done by pressing a 'Submit' button */
  def createInputArea(): Div =
    val enclosure = document.createElement("div").asInstanceOf[Div]  // Enclosure to contain all the elements for this area
    val label = createROText("Input TODO item:")                     // Label inviting the user to add TODO items
    val item = createREText("")                                      // Input field where the todo item is to be added
    item.id = "item"                                             // Name it so it can be access later from a global scope
  
    // Create a dropdown for priority selection
    val priority = document.createElement("select").asInstanceOf[Select]
    priority.id = "priority"
    val option1 = document.createElement("option").asInstanceOf[html.Option]
    option1.text = "1"
    val option2 = document.createElement("option").asInstanceOf[html.Option]
    option2.text = "2"
    val option3 = document.createElement("option").asInstanceOf[html.Option]
    option3.text = "3"
    Seq(option1, option2, option3).foreach(option => priority.add(option))
    // For the priority dropdown
    priority.style.width = "100px"
    priority.style.height = "35px"
    priority.style.fontSize = "20px"
    priority.style.margin = "10px 10px "
  
  
    // Create a date input for deadline selection
    val deadline = document.createElement("input").asInstanceOf[Input]
    deadline.id = "deadline"
    deadline.`type` = "date"
    // For the deadline input
    deadline.style.width = "150px"
    deadline.style.height = "35px"
    deadline.style.fontSize = "20px"
    deadline.style.margin = "10px 10px "
  
    val submit = createButton("Submit", submitAction)                  // Create the submit button, with a submit action to be executed when clicked
  
    Seq(label, item, priority, deadline, submit).foreach(enclosure.appendChild) // Add all the components to the enclosure         // Add all the components to the enclosure
    enclosure.style.background = "lightblue"                         // Use a distinct color to make this area stand out visually
    enclosure                                                        // Return the enclosure, which is to be added to the workspace


  def createLoginArea(onRegister: UIEvent => Unit, onLogin: UIEvent => Unit, onLogout: UIEvent => Unit): Div = {
    token = localStorage.getItem("token")
    tokenExpiration = getTokenExpiration()
    userId = localStorage.getItem("userId").toInt
    username = localStorage.getItem("username")

    var label = createROText(" you have logged in")
    val usernameInput = createREText("Enter username")
    usernameInput.id = "username"
    val emailInput = createREText("Enter email")
    emailInput.id = "email"
    val passwordInput = createREText("Enter password")
    passwordInput.id = "password"

    lazy val register: Button = createButton("Register", (event: UIEvent) => { onRegister(event); toggleVisibility(isLoggedIn = true) })
    lazy val login: Button = createButton("Login", (event: UIEvent) => { onLogin(event); toggleVisibility(isLoggedIn = true) })
    lazy val logout: Button = createButton("Logout", (event: UIEvent) => { onLogout(event); toggleVisibility(isLoggedIn = false) })

    def toggleVisibility(isLoggedIn: Boolean): Unit = {
      label.style.display = if (isLoggedIn) "inline-block" else "none"
      usernameInput.style.display = if (isLoggedIn) "none" else "inline-block"
      usernameInput.style.minWidth = if (isLoggedIn) "none" else "300px"
      emailInput.style.display = if (isLoggedIn) "none" else "inline-block"
      emailInput.style.minWidth = if (isLoggedIn) "none" else "300px"
      passwordInput.style.display = if (isLoggedIn) "none" else "inline-block"
      passwordInput.style.minWidth = if (isLoggedIn) "none" else "300px"
      register.style.display = if (isLoggedIn) "none" else "inline-block"
      login.style.display = if (isLoggedIn) "none" else "inline-block"
      logout.style.display = if (isLoggedIn) "inline-block" else "none"
    }

    val enclosure = document.createElement("div").asInstanceOf[Div]

    val nowaday = new js.Date()

    tokenExpiration match {
      case Some(date) =>
        if (date.getTime() - nowaday.getTime() >= 0) {
          toggleVisibility(isLoggedIn = true)
          label = createROText(s" User: $username has logged in")
        } else {
          toggleVisibility(isLoggedIn = false)
          alert("token has already expired! Please login again.")
        }
      case None =>
        alert("Please login or register.")
        toggleVisibility(isLoggedIn = false)
    }

    Seq(label, usernameInput, emailInput, passwordInput, register, login, logout).foreach(enclosure.appendChild)

    enclosure.style.background = "lightpink"
    enclosure
  }

  /** Create search input area */
  def createSearchArea(): Div =
    val enclosure = document.createElement("div").asInstanceOf[Div]
    val label = createROText("Search:")
    val searchInput = createREText("input key words or priority")
    searchInput.id = "searchInput"
    val searchButton = createButton("Search", searchAction)
    val clearButton = createButton("Clear all search", clearAction)
    Seq(label, searchInput, searchButton, clearButton).foreach(enclosure.appendChild)
    enclosure.style.background = "lightgreen"
    val searchPanel = document.createElement("div").asInstanceOf[Div] // create the panel containing the search results
    searchPanel.id = "searchPanel" // name it so it can be accessed later
    searchPanel.style.background = "lightgreen" // give it a distinguishing color so that it is easily identifiable visually
    enclosure.appendChild(searchPanel)
    enclosure



  def createFilterArea(): Div =
    val enclosure = document.createElement("div").asInstanceOf[Div]  // Enclosure to contain all the elements for this area
    val label = createROText("Filter todos by keywords:")                     // Label inviting the user to add TODO items
    val keywordInput = createREText("")                                      // Input field where the todo item is to be added
    keywordInput.id = "keywordInput"                                             // Name it so it can be access later from a global scope
    // Create a dropdown for priority selection
    val priority = document.createElement("select").asInstanceOf[Select]
    priority.id = "priority"
    val option1 = document.createElement("option").asInstanceOf[html.Option]
    option1.text = "1"
    val option2 = document.createElement("option").asInstanceOf[html.Option]
    option2.text = "2"
    val option3 = document.createElement("option").asInstanceOf[html.Option]
    option3.text = "3"
    Seq(option1, option2, option3).foreach(option => priority.add(option))
    // For the priority dropdown
    priority.style.width = "100px"
    priority.style.height = "35px"
    priority.style.fontSize = "20px"
    priority.style.margin = "10px 10px "

    val filter = createButton("Filter", filterAction)
    val showAll = createButton("Reload all todos", showAllAction) // Create the submit button, with a submit action to be executed when clicked

    Seq(label, keywordInput, priority, filter, showAll).foreach(enclosure.appendChild) // Add all the components to the enclosure         // Add all the components to the enclosure
    enclosure.style.background = "lightorange"                         // Use a distinct color to make this area stand out visually
    enclosure                                                        // Return the enclosure, which is to be added to the workspace


  def filterAction (event: UIEvent): Unit =
    {
      if (token != null) {
        val itemText = document.getElementById("keywordInput").innerText
        val priorityText = document.getElementById("priority").asInstanceOf[Select].value
        updatePanel(contentAction(_, itemText, priorityText))()

      }

    }

  def showAllAction(event: UIEvent): Unit = {
    updatePanel(contentAction(_, "", ""))()
  }

  /** Action to be executed when the search button is clicked. */
  def searchAction = (_: UIEvent) =>
    token = localStorage.getItem("token")
    tokenExpiration = getTokenExpiration()
    userId = localStorage.getItem("userId").toInt
    if (token != null) {
      val searchText = document.getElementById("searchInput").innerText
      val header1 = js.Dictionary("Authorization" -> s"$token")
      if searchText != null && searchText.nonEmpty
      then
        fetch("/search", new RequestInit {
          method = HttpMethod.POST
          headers = header1
          body = searchText
        }).toFuture.foreach { response =>
          response.text().toFuture.foreach { searchResults =>
            updateSearchResults(searchResults)
          }
        }
        document.getElementById("searchInput").innerText = ""
      else
        ()
      ()
    }else {
      alert("please login or register first.")
    }

  def clearAction = (_: UIEvent) => {
    token = localStorage.getItem("token")
    tokenExpiration = getTokenExpiration()
    userId = localStorage.getItem("userId").toInt
    val panel = document.getElementById("searchPanel").asInstanceOf[Div]
    // Clear the panel before appending new elements
    while (panel.firstChild != null) {
      panel.removeChild(panel.firstChild)
    }
  }

  def updateSearchResults(searchResults: String): Unit = {
    token = localStorage.getItem("token")
    tokenExpiration = getTokenExpiration()
    userId = localStorage.getItem("userId").toInt
    val searchItems = JSON.parse(searchResults).selectDynamic("items").asInstanceOf[Array[js.Dynamic]]

    val elements = searchItems.map { itemData =>
      val  text = itemData.item.asInstanceOf[String]
      val priority = itemData.priority.asInstanceOf[String]
      val deadline = itemData.deadline.asInstanceOf[String]
      val isFinished = itemData.finished.asInstanceOf[Boolean]
      val isOverdue = itemData.overdue.asInstanceOf[Boolean]
      val itemText = createROText(text)
      itemText.style.display = "inline-block"
      itemText.style.minWidth = "675px"
      val priorityText = createROText("Priority: " + priority)
      priorityText.style.display = "inline-block"
      priorityText.style.minWidth = "100px"
      priorityText.style.fontSize = "24px"

      val deadlineText = createROText("Deadline: " + deadline)
      deadlineText.style.display = "inline-block"
      deadlineText.style.minWidth = "150px"
      deadlineText.style.fontSize = "24px"


      itemText.style.display = "inline-block"  // stack from left to right, instead of top-bottom                                                                                                                  sssssssssssssss
      val container = document.createElement("div").asInstanceOf[Div] // each item + delete bottom goes into a separate container, whose componets stack horizontally


      List( itemText, priorityText, deadlineText).foreach(container.appendChild) // add the item and button to the container
      container
    }.to(Seq)

    val panel = document.getElementById("searchPanel").asInstanceOf[Div]
    // Clear the panel before appending new elements
    while (panel.firstChild != null) {
      panel.removeChild(panel.firstChild)
    }
    // Append new elements to the panel
    elements.foreach { element =>
      panel.appendChild(element)
    }
  }



  def submitAction: UIEvent => Unit = _ => {
    token = localStorage.getItem("token")
    tokenExpiration = getTokenExpiration()
    userId = localStorage.getItem("userId").toInt
    println(s"Token value in submit: $token")
    if (token != null) {
      val itemText = document.getElementById("item").innerText
      val priorityText = document.getElementById("priority").asInstanceOf[Select].value
      val deadlineText = document.getElementById("deadline").asInstanceOf[Input].value

      val todoData = js.Dynamic.literal(
        item = itemText,
        priority = priorityText,
        deadline = deadlineText
      )

      val json = JSON.stringify(todoData)

      val headers1 = js.Dictionary("Authorization" -> s"$token", "Content-Type" -> "application/json")

      if (itemText != null && priorityText != null && deadlineText != null) {
        fetch("/submit", new RequestInit { // Async request to the backend to delete the item from the DB
          method = HttpMethod.POST
          headers = headers1
          body = json
        })
        window.setTimeout(updatePanel(contentAction(_, "", "")),100) // After a small timeout to allow the DB transaction to complete, refresh the panel to allow the changes to be reflected.
        document.getElementById("item").innerText = ""
        document.getElementById("priority").asInstanceOf[Select].value = ""
        document.getElementById("deadline").asInstanceOf[Input].value = ""
        ()
      } else {
        alert("please fill in all fields.")
      }
    } else {
      alert("please login or register first.")
    }
  }


  
  /** We update the panel on every change to the DB */
  def updatePanel(contentAction: String => Unit) = () => fetchContent(contentAction)



  /** Asynchronously fetch all the todos, so that refreshing the panel becomes possible */
  def fetchContent(contentAction: String => Unit) =
    token = localStorage.getItem("token")
    tokenExpiration = getTokenExpiration()
    userId = localStorage.getItem("userId").toInt
    if (token != null) {
      val header1 = js.Dictionary("Authorization" -> s"$token","Content-Type" -> "application/json;charset=UTF-8")

      val contentFuture = for {
        response <- fetch("/readtodos", new RequestInit {
          method = HttpMethod.GET
          headers = header1
        })
        text <- response.text()
      } yield text

      for {
        content <- contentFuture
      } yield {
        val parsedContent = ujson.read(content)

        if (parsedContent.obj.contains("status") && parsedContent("status").str == "error") {
          val panel = document.getElementById("panel").asInstanceOf[Div]
          // Clear the panel before appending new elements
          while (panel.firstChild != null) {
            panel.removeChild(panel.firstChild)
          }
        } else {
          alert("successful")
          contentAction(content)
        }
      }
    } else {
      alert("please login or register first.")
    }


  /**
   * Update the panel by creating a visual element for every item fetched from the database. Add a 'Delete' btn to each items container.
   */
  def contentAction(content: String, filterItemText: String = "", filterPriorityText: String = ""): Unit = {
    token = localStorage.getItem("token")
    tokenExpiration = getTokenExpiration()
    userId = localStorage.getItem("userId").toInt
    val payload = JSON.parse(content).selectDynamic("items").asInstanceOf[Array[js.Dynamic]]
    /** Put the visual elements containing the items into a list that will replace the current panel elements */

    val elements = payload.filter { itemData =>
      val itemFilter = if (filterItemText.nonEmpty) itemData.item.asInstanceOf[String].toLowerCase.contains(filterItemText.toLowerCase) else true
      val priorityFilter = if (filterPriorityText.nonEmpty) itemData.priority.asInstanceOf[String] == filterPriorityText else true
      itemFilter && priorityFilter
    }.map {  itemData =>
      val  id = itemData.id.asInstanceOf[String]
      val  text = itemData.item.asInstanceOf[String]
      val priority = itemData.priority.asInstanceOf[String]
      val deadline = itemData.deadline.asInstanceOf[String]
      val isFinished = itemData.finished.asInstanceOf[Boolean]
      val isOverdue = itemData.overdue.asInstanceOf[Boolean]
      val isUrgent = itemData.urgent.asInstanceOf[Boolean]
      val itemText = createROText(text)
      itemText.style.display = "inline-block"
      itemText.style.minWidth = "675px"
      val priorityText = createROText("Priority: " + priority)
      priorityText.style.display = "inline-block"
      priorityText.style.minWidth = "100px"
      priorityText.style.fontSize = "24px"

      val deadlineText = createROText("Deadline: " + deadline)
      deadlineText.style.display = "inline-block"
      deadlineText.style.minWidth = "150px"
      deadlineText.style.fontSize = "24px"

      val finishedCheckbox = document.createElement("input").asInstanceOf[html.Input]
      finishedCheckbox.`type` = "checkbox"
      finishedCheckbox.checked = isFinished
      finishedCheckbox.style.width = "20px"
      finishedCheckbox.addEventListener("change", finishAction(id))

      val deleteBtn = createButton("Delete", deleteAction(id))  // the button is specialized to delete only the current item
      itemText.style.display = "inline-block"  // stack from left to right, instead of top-bottom

      val redoBtn = createButton("Redo Today", redoAction(text, priority))  // the button is specialized to delete only the current item
      itemText.style.display = "inline-block"


      val container = document.createElement("div").asInstanceOf[Div] // each item + delete bottom goes into a separate container, whose componets stack horizontally

      val upPriorityBtn = createButton("Up Priority", upPriorityAction(id,priority))  // the button is specialized to delete only the current item
      itemText.style.display = "inline-block"  // stack from left to right, instead of top-bottom

      val downPriorityBtn = createButton("Down Priority", downPriorityAction(id,priority))  // the button is specialized to delete only the current item
      itemText.style.display = "inline-block"  // stack from left to right, instead of top-bottom

      if (finishedCheckbox.checked || isFinished) {
        priorityText.style.textDecoration = "line-through"
        deadlineText.style.textDecoration = "line-through"
        itemText.style.textDecoration = "line-through"
      } else {
        priorityText.style.textDecoration = "none"
        deadlineText.style.textDecoration = "none"
        itemText.style.textDecoration = "none"
        if (isOverdue) {
          deadlineText.style.color = "lightgrey"
          priorityText.style.color = "lightgrey"
          itemText.style.color = "lightgrey"
        } else if (isUrgent) {
          deadlineText.style.color = "red"
          priorityText.style.color = "red"
          itemText.style.color = "red"
        }else
          {
            if (priority == "1")
              {
                deadlineText.style.color = "green"
                priorityText.style.color = "green"
                itemText.style.color = "green"
              }
            else if (priority == "2")
            {
              deadlineText.style.color = "blue"
              priorityText.style.color = "blue"
              itemText.style.color = "blue"
            }else
              {
                deadlineText.style.color = "pink"
                priorityText.style.color = "pink"
                itemText.style.color = "pink"
              }
          }
      }
      List(finishedCheckbox, itemText, priorityText, upPriorityBtn, downPriorityBtn, deadlineText, deleteBtn, redoBtn).foreach(container.appendChild) // add the item and button to the container
      container
    }.to(Seq) // return a list item containers
    val panel = document.getElementById("panel").asInstanceOf[Div]
    // Clear the panel before appending new elements
    while (panel.firstChild != null) {
      panel.removeChild(panel.firstChild)
    }
    // Append new elements to the panel
    elements.foreach { element =>
      panel.appendChild(element)
    }

  }
  
  def finishAction(textid: String): UIEvent => Unit = (event: UIEvent) =>
    token = localStorage.getItem("token")
    tokenExpiration = getTokenExpiration()
    userId = localStorage.getItem("userId").toInt
    if (token != null)
    {
      val header1 = js.Dictionary("Authorization" -> s"$token")
      val checkbox = event.target.asInstanceOf[html.Input]
      val isFinished = checkbox.checked
      val isOverdue = isFinished
      val finishData = js.Dynamic.literal(
        id = textid,
        finished = isFinished,
        overdue = isOverdue,
      )
      val json = JSON.stringify(finishData)

      fetch("/finish", new RequestInit {
        method = HttpMethod.PUT
        headers = header1
        body = json
      })
      window.setTimeout(updatePanel(contentAction(_, "", "")),100)// After a small timeout to allow the DB transaction to complete, refresh the panel to allow the changes to be reflected.
    } else {
      alert("please login or register first.")
    }

  def upPriorityAction(textid: String, priorityText:String): UIEvent => Unit = (event: UIEvent) =>
    token = localStorage.getItem("token")
    tokenExpiration = getTokenExpiration()
    userId = localStorage.getItem("userId").toInt
    if (token != null)
    {
      if (priorityText.toInt == 1 )
      {
        alert("Could not upgrade priority. Already at the highest priority level.")
      }
      else {
        val header1 = js.Dictionary("Authorization" -> s"$token")
        val upPriorityData = js.Dynamic.literal(
          id = textid,
          priority = (priorityText.toInt - 1).toString
        )
        val json = JSON.stringify(upPriorityData)

        fetch("/upPriority", new RequestInit {
          method = HttpMethod.PUT
          headers = header1
          body = json
        })
        window.setTimeout(updatePanel(contentAction(_, "", "")),100) // After a small timeout to allow the DB transaction to complete, refresh the panel to allow the changes to be reflected.
      }
    } else {
      alert("please login or register first.")
    }

  def downPriorityAction(textid: String, priorityText:String): UIEvent => Unit = (event: UIEvent) =>
    token = localStorage.getItem("token")
    tokenExpiration = getTokenExpiration()
    userId = localStorage.getItem("userId").toInt
    if (token != null)
    {
      if (priorityText.toInt == 3 )
      {
        alert("Could not degrade priority. Already at the lowest priority level.")
      }
      else {
        val header1 = js.Dictionary("Authorization" -> s"$token")
        val downPriorityData = js.Dynamic.literal(
          id = textid,
          priority = (priorityText.toInt + 1).toString
        )
        val json = JSON.stringify(downPriorityData)

        fetch("/downPriority", new RequestInit {
          method = HttpMethod.PUT
          headers = header1
          body = json
        })
        window.setTimeout(updatePanel(contentAction(_, "", "")),100) // After a small timeout to allow the DB transaction to complete, refresh the panel to allow the changes to be reflected.
      }
    } else {
      alert("please login or register first.")
    }
  
  /** Delete action to go into the Delete button for every item */
  def deleteAction(itemid: String): UIEvent => Unit = _ =>
    token = localStorage.getItem("token")
    tokenExpiration = getTokenExpiration()
    userId = localStorage.getItem("userId").toInt
    if (token != null)
    {
      val header1 = js.Dictionary("Authorization" -> s"$token")
      val deleteData = js.Dynamic.literal(
        id = itemid
      )
      val json = JSON.stringify(deleteData)
      fetch("/delete", new RequestInit { // Async request to the backend to delete the item from the DB
        method = HttpMethod.DELETE
        headers = header1
        body = json
      })
      window.setTimeout(updatePanel(contentAction(_, "", "")),100) // After a small timeout to allow the DB transaction to complete, refresh the panel to allow the changes to be reflected.
      ()
    } else {
      alert("please login or register first.")
    }


  def redoAction(text: String, priority:String): UIEvent => Unit = _ =>
    token = localStorage.getItem("token")
    tokenExpiration = getTokenExpiration()
    userId = localStorage.getItem("userId").toInt
    if (token != null)
    {
      val header1 = js.Dictionary("Authorization" -> s"$token")
      val deleteData = js.Dynamic.literal(
        item = text,
        priority = priority
      )
      val json = JSON.stringify(deleteData)
      fetch("/redo", new RequestInit { // Async request to the backend to delete the item from the DB
        method = HttpMethod.POST
        headers = header1
        body = json
      })
      window.setTimeout(updatePanel(contentAction(_, "", "")),100) // After a small timeout to allow the DB transaction to complete, refresh the panel to allow the changes to be reflected.
      ()
    } else {
      alert("please login or register first.")
    }


  def registerAction : UIEvent => Unit = (_: UIEvent) =>
    val usernameText = document.getElementById("username").innerText
    val emailText = document.getElementById("email").innerText
    val passwordText = document.getElementById("password").innerText
    val userData = js.Dynamic.literal(
      username = usernameText,
      email = emailText,
      password = passwordText,
    )
    val json = JSON.stringify(userData)

    try {
      if usernameText.nonEmpty && emailText.nonEmpty && passwordText.nonEmpty
      then
        val responsePromise = fetch("/register", new RequestInit {
          method = HttpMethod.POST
          headers = new Headers(js.Dictionary(
            "Content-Type" -> "application/json;charset=UTF-8"
          ))
          body = json
        })
        responsePromise
          .toFuture
          .flatMap(_.json().toFuture)
          .foreach { json =>
            val response = json.asInstanceOf[js.Dictionary[Any]]
            if (response("status").asInstanceOf[String] == "success") {
              token = response("token").asInstanceOf[String]
              tokenExpiration = Some(new js.Date(response("tokenExpiration").asInstanceOf[String]))
              userId = response("userId").asInstanceOf[Int]
              username = usernameText
              localStorage.setItem("token", token)
              localStorage.setItem("tokenExpiration", tokenExpiration.toString)
              localStorage.setItem("userId", userId.toString)
              localStorage.setItem("username", username)
              alert(s"Register and logged in as user with ID: $userId")
            } else {
              alert("Authentication failed.")
            }
          }
        document.getElementById("username").innerText = ""
        document.getElementById("email").innerText = ""
        document.getElementById("password").innerText = ""
      else
        alert("please fullfill input blanks.")
    } finally {
      window.setTimeout(updatePanel(contentAction(_, "", "")),100) // After a small timeout to allow the DB transaction to complete, refresh the panel to allow the changes to be reflected.
      ()
    }


  def loginAction : UIEvent => Unit = (_: UIEvent) =>
    val usernameText = document.getElementById("username").innerText
    val emailText = document.getElementById("email").innerText
    val passwordText = document.getElementById("password").innerText
    val userData = js.Dynamic.literal(
      username = usernameText,
      email = emailText,
      password = passwordText,
    )
    val json = JSON.stringify(userData)

    try {
      if usernameText.nonEmpty && emailText.nonEmpty && passwordText.nonEmpty
      then
        val responsePromise = fetch("/login", new RequestInit {
          method = HttpMethod.POST
          headers = new Headers(js.Dictionary(
            "Content-Type" -> "application/json;charset=UTF-8"
          ))
          body = json
        })
        responsePromise
          .toFuture
          .flatMap(_.json().toFuture)
          .foreach { json =>
            val response = json.asInstanceOf[js.Dictionary[Any]]
            if (response("status").asInstanceOf[String] == "success") {
              token = response("token").asInstanceOf[String]
              tokenExpiration = Some(new js.Date(response("tokenExpiration").asInstanceOf[String]))
              userId = response("userId").asInstanceOf[Int]
              username = usernameText
              localStorage.setItem("token", token)
              localStorage.setItem("tokenExpiration", tokenExpiration.toString)
              localStorage.setItem("userId", userId.toString)
              localStorage.setItem("username", username)
              alert(s"Logged in as user with ID: $userId")
            } else {
              alert("Authentication failed.")
            }
          }
        document.getElementById("username").innerText = ""
        document.getElementById("email").innerText = ""
        document.getElementById("password").innerText = ""
      else
        alert("please fullfill input blanks.")
    } finally {

      window.setTimeout(updatePanel(contentAction(_, "", "")),100) // After a small timeout to allow the DB transaction to complete, refresh the panel to allow the changes to be reflected.
      ()
    }


  /** Delete action to go into the Delete button for every item */
  def logoutAction : UIEvent => Unit = (_: UIEvent) => {
    token = localStorage.getItem("token")
    tokenExpiration = getTokenExpiration()
    userId = localStorage.getItem("userId").toInt
    if (token != null) {
      val header1 = js.Dictionary("Authorization" -> s"$token", "Content-Type" -> "application/json;charset=UTF-8")
      val userData = js.Dynamic.literal(
        token = token
      )
      val json = JSON.stringify(userData)
      try{
          fetch("/logout", new RequestInit {
          method = HttpMethod.POST
          headers = header1
          body = json
        }).toFuture
          .flatMap(_.json().toFuture)
          .foreach { json =>
            val response = json.asInstanceOf[js.Dictionary[Any]]
            if (response("status").asInstanceOf[String] == "success") {
              token = ""
              tokenExpiration = None
              userId = 0 // Add this line
              username = ""
              localStorage.setItem("token", token)
              localStorage.setItem("tokenExpiration", tokenExpiration.toString)
              localStorage.setItem("userId", userId.toString)
              localStorage.setItem("username", username)
              alert("Logout successfully")
            } else {
              alert("Invalid or expired token")
            }
          }
        } finally{
        window.setTimeout(updatePanel(contentAction(_, "", "")),100) }// After a small timeout to allow the DB transaction to complete, refresh the panel to allow the changes to be reflected.
      } else {
        alert("please login or register first.")
    }
  }
  
  
  
  
