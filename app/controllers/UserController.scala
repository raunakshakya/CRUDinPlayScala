package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import anorm._
import play.api.Play.current

import views._
import models._
import play.api.db.DB

object UserController extends Controller {

  val signUpForm: Form[User] = Form(
    mapping(
      "firstName" -> text,
      "lastName" -> text,
      "email" -> text,
      "phone" -> text,
      "company" -> text,
      "username" -> nonEmptyText,
      "password" -> text(minLength = 6)
    )(User.apply)(User.unapply)
  )

  val signInForm: Form[SignInData] = Form(
    mapping(
      "email" -> email,
      "password" -> text
    )(SignInData.apply)(SignInData.unapply)
  )

  /**
   * Create a new User instance
   */
  def createUser = Action {
    implicit request =>
      signUpForm.bindFromRequest.fold(
        errors => BadRequest(html.home.home("Errors")),
        user => {
          DB.withConnection {
            implicit c =>
              SQL(
                """
                INSERT INTO User(firstName, lastName, email, phone, company, username, password) VALUES
                  ({firstName}, {lastName}, {email}, {phone}, {company}, {username}, {password})
                """).on(
                'firstName -> user.firstName,
                'lastName -> user.lastName,
                'email -> user.email,
                'phone -> user.phone,
                'company -> user.company,
                'username -> user.username,
                'password -> user.password
              ).executeUpdate()
          }
          Ok(html.home.home("New user created!"))
        }
      )
  }

  /**
   * Update the existing User instance
   */
  def updateUser(user: User): User = {
    user
  }

  /**
   * Authenticate a User.
   */
  def authenticate = Action {
    implicit request =>
      signInForm.bindFromRequest.fold(
        errors => Redirect(routes.Application.index), //BadRequest(html.home.home("Errors")),
        signInData =>
          DB.withConnection {
            implicit c =>
              val simpleSql = SQL(
                """
                  SELECT COUNT(*) FROM User WHERE email = {email} AND password = {password}
                """
              ).on(
                'email -> signInData.email,
                'password -> signInData.password
              )

              val count: Long = simpleSql.as(SqlParser.scalar[Long].single)
              if (count > 1) {
                throw new IllegalStateException("More than one user with same email found!!!")
              }
              if (count == 1) {
                Ok(html.home.home("Logged in"))
              } else {
                Redirect(routes.Application.index)
              }
          }
      )
  }

}
