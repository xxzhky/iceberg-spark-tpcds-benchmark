/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.execution.benchmark

import org.apache.spark.SparkFunSuite
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.test.SharedSparkSession

import java.io.{File, FilenameFilter}
import scala.collection.JavaConverters._

class TPCDSDatagenSuite extends SparkFunSuite with SharedSparkSession {

  test("datagen") {
    val outputTempDir = Utils.createTempDir()
    val tpcdsTables = new Tables(spark.sqlContext, 1)
    tpcdsTables.genData(
      GenDataConfig(
        outputTempDir.getAbsolutePath,
        "parquet",
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        Set.empty,
        4
      )
    )

    val tpcdsExpectedTables = Set(
      "call_center", "catalog_page", "catalog_returns", "catalog_sales", "customer",
      "customer_address", "customer_demographics", "date_dim", "household_demographics",
      "income_band", "inventory", "item", "promotion", "reason", "ship_mode", "store",
      "store_returns", "store_sales", "time_dim", "warehouse", "web_page", "web_returns",
      "web_sales", "web_site")

    assert(outputTempDir.list.toSet === tpcdsExpectedTables)

    // Checks if output test data generated in each table
    val filenameFilter = new FilenameFilter {
      override def accept(dir: File, name: String): Boolean = name.endsWith(".parquet")
    }
    tpcdsExpectedTables.foreach { table =>
      val f = new File(s"${outputTempDir.getAbsolutePath}/$table").listFiles(filenameFilter)
      assert(f.size === 1)
    }
  }

  test("iceberg_spark") {
    val spark = SparkSession
      .builder()
      .appName("Java Spark SQL basic example")
      .config("spark.sql.extensions", "org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions")
      .config("spark.sql.catalog.spark_catalog", "org.apache.iceberg.spark.SparkSessionCatalog")
      .config("spark.sql.catalog.spark_catalog.type", "hive")
      .config("spark.sql.catalog.hadoop_prod", "org.apache.iceberg.spark.SparkCatalog")
      .config("spark.sql.catalog.hadoop_prod.type", "hadoop")
      .config("spark.sql.catalog.hadoop_prod.warehouse", "file:///tmp/iceberg_hadoop")
      //   .config("warehouse", "file:///D:/flink/sms_iceberg")
      .master("local[2]")
      .getOrCreate();

    val dataFrame = spark.read.format("iceberg").load("file:///tmp/iceberg_hadoop/store_sales")
    dataFrame.show(20)

  }

  // TODO: Adds tests to check the schemas of generated tables
}
