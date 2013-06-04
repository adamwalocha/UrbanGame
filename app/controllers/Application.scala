/**Copyright 2013 BLStream, BLStream's Patronage Program Contributors
 *     http://blstream.github.com/UrbanGame/
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.Play.current

object Application extends Controller with CookieLang {
  
  def index = Action { implicit request =>
    Ok(Scalate("index").render('title -> "Urban Game"))
  }

  val loginForm = Form(
    tuple(
      "login" -> nonEmptyText,
      "password" -> nonEmptyText
    )
  )

  def login = Action { implicit request =>
    Ok("login")
  }

  def processLogin = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      errors => BadRequest(Scalate("index").render('title -> "Urban Game", 'errors -> errors)),
      { case (login, password) => 
          Redirect(routes.GamesCtrl.myGames)
      }
    )
  }

  def logout = Action { implicit request =>
    Ok(Scalate("logout").render('title -> "Urban Game - Logout", 'request -> request))
  }

  import play.api.db.slick.Config.driver.simple._
  import play.api.db.DB
  import play.api.Play.current
  import scala.slick.session.Database
  import models.utils._
  import models.dal.Bridges._
  import play.api.libs.json._
  import play.api.libs.functional.syntax._
  
  def register = Action { implicit request =>
    val od = OperatorsData(None, "op", "pass")

    val opId = play.api.db.slick.DB.withSession { implicit session =>
      Operators.createAccount(od)
    }

    Ok(Json.toJson(opId))
  }

  def fillDatabase = Action { implicit request =>
    import scala.io._
    import com.github.nscala_time.time.Imports._

    val filepaths: List[String] = List("app/initData/games.txt","app/initData/operators.txt")
    var cnt1 = 0
    var cnt2 = 0
    var cnt3 = 0
        
    play.api.db.slick.DB.withSession { implicit session =>
      if (Operators.getRowsNo == 0) {
        Source.fromFile(filepaths(1)).getLines.foreach { line => 
          val List(uname, pass) = line.split("::").map(_.toString).toList

          val od = OperatorsData(None, uname, pass)

          Operators.createAccount(od)
          cnt2 = cnt2 + 1
        }
      }

      if (Games.getRowsNo == 0) {
        Source.fromFile(filepaths(0)).getLines.foreach { line => 
          val List(name, version, description, location, operatorId, created, startTime, 
            endTime, started, ended, winning, nWins, difficulty, maxPlayers, awards, 
            status, image) = line.split("::").map(_.toString).toList

          val gd = GamesDetails(None, name, version.toInt, description, location, operatorId.toInt, new DateTime(created), 
            DateTime.now, new DateTime(startTime), new DateTime(endTime), Some(new DateTime(started)), Some(new DateTime(ended)), 
            winning, nWins.toInt, difficulty, maxPlayers.toInt, awards, status, image)

          Games.createGame(gd)
          cnt1 = cnt1 + 1
        }
      }

      if (Tasks.getRowsNo == 0) {
        val td = TasksDetails(None, 1, 1, "Task1", "Task1 desc", DateTime.now + 2.days, 100, 20)

        Tasks.createTask(td)
        cnt3 = cnt3 + 1
      }
    }

    Ok("Inserted " + cnt1 + " game(s) and " + cnt2 + " operator(s) and " + cnt3 + " task(s)")
  }

  def jsMessages = Action { implicit request =>
    import jsmessages.api.JsMessages
    Ok(JsMessages.apply(Some("Messages"))).as(JAVASCRIPT)
  }

}