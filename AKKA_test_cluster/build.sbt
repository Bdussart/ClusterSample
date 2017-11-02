name := "AKKA_test_cluster"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.6",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.6",
  "com.typesafe.akka" %% "akka-remote" % "2.5.6",
  "com.typesafe.akka" %% "akka-cluster" % "2.5.6",
  "com.typesafe.akka" %% "akka-cluster-tools" % "2.5.6",
  "com.typesafe.akka" %% "akka-cluster-sharding" % "2.5.6",
  "com.typesafe.akka" %% "akka-persistence" % "2.5.6",
  "com.typesafe.akka" %% "akka-contrib" % "2.5.6",
  "org.iq80.leveldb" % "leveldb" % "0.8",
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8")