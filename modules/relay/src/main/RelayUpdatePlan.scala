package lila.relay

import lila.study.Chapter
import lila.study.Chapter.Order

object RelayUpdatePlan:

  case class Input(chapters: List[Chapter], games: RelayGames):
    override def toString: String =
      s"Input(chapters = ${chapters.map(_.name)}, games = ${games.map(_.tags.names)})"

  case class Output(
      reorder: Option[List[StudyChapterId]],
      update: List[(Chapter, RelayGame)],
      append: RelayGames
  ):
    override def toString: String =
      s"Output(reorder = $reorder, update = ${update.map(_._1.name)}, append = ${append.map(_.tags.names)})"

  case class Plan(input: Input, output: Output)

  def apply(chapters: List[Chapter], games: RelayGames): Plan =
    apply(Input(chapters, games))

  def apply(input: Input): Plan =
    import input.*

    val tagMatches: List[(RelayGame, Chapter)] =
      games
        .flatMap: game =>
          chapters.collect:
            case chapter if game.isSameGame(chapter.tags) => game -> chapter
        .toList

    val replaceInitialChapter: Option[(RelayGame, Chapter)] =
      chapters match
        case List(only) if only.isEmptyInitial =>
          // tagMatches should be empty
          games.headOption.map(_ -> only)
        case _ => none

    val updates: List[(Chapter, RelayGame)] = replaceInitialChapter match
      case Some(initial) => List(initial.swap)
      case None          => tagMatches.map(_.swap)

    val appends: Vector[RelayGame] = games.filterNot: g =>
      updates.exists(_._2 == g)

    // requires all chapters to be updated
    // contains all chapter ids
    val reorder: Option[List[StudyChapterId]] =
      val ids = updates.map(_._1.id)
      Option.when(ids.size == chapters.size && ids != chapters.map(_.id).pp(ids))(ids)

    val output = Output(
      reorder = reorder,
      update = updates,
      append = appends
    )
    Plan(input, output)
