// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package org.dromara.dbswitch.data.service;

import cn.hutool.core.stream.StreamUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.StrUtil;
import org.dromara.dbswitch.common.entity.CloseableDataSource;
import org.dromara.dbswitch.common.entity.LoggingSupplier;
import org.dromara.dbswitch.common.entity.MdcKeyValue;
import org.dromara.dbswitch.core.basic.robot.RobotReader;
import org.dromara.dbswitch.data.config.DbswichPropertiesConfiguration;
import org.dromara.dbswitch.data.domain.ReaderTaskParam;
import org.dromara.dbswitch.data.domain.ReaderTaskResult;
import org.dromara.dbswitch.data.entity.SourceDataSourceProperties;
import org.dromara.dbswitch.data.handler.ReaderTaskThread;
import org.dromara.dbswitch.data.util.JsonUtils;
import org.dromara.dbswitch.core.schema.TableDescription;
import org.dromara.dbswitch.core.service.DefaultMetadataService;
import org.dromara.dbswitch.core.service.MetadataService;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.AsyncTaskExecutor;

/**
 * 源端数据库表并发读取控制
 *
 * @author tang
 */
@Slf4j
public class DefaultReaderRobot extends RobotReader<ReaderTaskResult> {

  private MdcKeyValue mdcKeyValue;
  private DbswichPropertiesConfiguration configuration;
  private CloseableDataSource sourceDataSource;
  private CloseableDataSource targetDataSource;
  private List<Supplier> readTaskThreads;
  private List<CompletableFuture> futures;
  private AsyncTaskExecutor threadExecutor;
  private CountDownLatch countDownLatch;

  public DefaultReaderRobot(MdcKeyValue mdcKeyValue,
      DbswichPropertiesConfiguration configuration,
      CloseableDataSource sourceDataSource,
      CloseableDataSource targetDataSource) {
    this.mdcKeyValue = mdcKeyValue;
    this.configuration = configuration;
    this.sourceDataSource = sourceDataSource;
    this.targetDataSource = targetDataSource;
  }

  @Override
  public void interrupt() {
    Optional.ofNullable(futures).orElseGet(ArrayList::new).forEach(f -> f.cancel(true));
    this.clearChannel();
  }

  @Override
  public void init(AsyncTaskExecutor threadExecutor) {
    this.threadExecutor = threadExecutor;
    this.readTaskThreads = new ArrayList<>();
    MetadataService tdsService = new DefaultMetadataService(targetDataSource, configuration.getTarget().getType());
    Set<String> targetExistTables = tdsService.queryTableList(configuration.getTarget().getTargetSchema())
        .stream().map(t -> t.getTableName().toLowerCase()).collect(Collectors.toSet());
    List<TableDescription> tableDescriptions = splitReaderTask();
    this.countDownLatch = new CountDownLatch(tableDescriptions.size());
    tableDescriptions.forEach(t -> addReaderTask(t, targetExistTables));
  }

  private void addReaderTask(TableDescription tableDescription, Set<String> targetExistTables) {
    ReaderTaskParam param = ReaderTaskParam.builder()
        .memChannel(this.getChannel())
        .tableDescription(tableDescription)
        .configuration(this.configuration)
        .sourceDataSource(sourceDataSource)
        .targetDataSource(targetDataSource)
        .targetExistTables(targetExistTables)
        .countDownLatch(countDownLatch)
        .build();
    if (Objects.nonNull(this.mdcKeyValue)) {
      this.readTaskThreads.add(new LoggingSupplier(new ReaderTaskThread(param), this.mdcKeyValue));
    } else {
      this.readTaskThreads.add(new ReaderTaskThread(param));
    }
  }

  private List<TableDescription> splitReaderTask() {
    List<TableDescription> tableDescriptions = new ArrayList<>();

    SourceDataSourceProperties sourceProperties = configuration.getSource();
    MetadataService sourceMetaDataService = new DefaultMetadataService(sourceDataSource, sourceProperties.getType());

    // 判断处理的策略：是排除还是包含
    List<String> includes =
        StreamUtil.of(StrUtil.split(sourceProperties.getSourceIncludes(), StrPool.COMMA))
            .collect(Collectors.toList());
    log.info("Includes tables is :{}", JsonUtils.toJsonString(includes));
    List<String> filters =
        StreamUtil.of(StrUtil.split(sourceProperties.getSourceExcludes(), StrPool.COMMA))
            .collect(Collectors.toList());
    log.info("Filter tables is :{}", JsonUtils.toJsonString(filters));

    boolean useExcludeTables = includes.isEmpty();
    if (useExcludeTables) {
      log.info("!!!! Use dbswitch.source.source-excludes parameter to filter tables");
    } else {
      log.info("!!!! Use dbswitch.source.source-includes parameter to filter tables");
    }

    List<String> schemas =
        StreamUtil.of(StrUtil.split(sourceProperties.getSourceSchema(), StrPool.COMMA))
            .collect(Collectors.toList());
    log.info("Source schema names is :{}", JsonUtils.toJsonString(schemas));

    for (String schema : schemas) {
      List<TableDescription> tableList = sourceMetaDataService.queryTableList(schema);
      if (tableList.isEmpty()) {
        log.warn("### Find source database table list empty for schema name is : {}", schema);
      } else {
        String allTableType = sourceProperties.getTableType();
        for (TableDescription td : tableList) {
          // 当没有配置迁移的表名时，默认为根据类型同步所有
          if (includes.isEmpty()) {
            if (null != allTableType && !allTableType.equals(td.getTableType())) {
              continue;
            }
          }

          String tableName = td.getTableName();
          if (useExcludeTables) {
            if (!filters.contains(tableName)) {
              tableDescriptions.add(td);
            }
          } else {
            if (includes.size() == 1 && (includes.get(0).contains("*") || includes.get(0).contains("?"))) {
              if (Pattern.matches(includes.get(0), tableName)) {
                tableDescriptions.add(td);
              }
            } else if (includes.contains(tableName)) {
              tableDescriptions.add(td);
            }
          }
        }
      }
    }
    return tableDescriptions;
  }

  @Override
  public void startRead() {
    futures = new ArrayList<>(readTaskThreads.size());
    readTaskThreads.forEach(
        task ->
            futures.add(CompletableFuture.supplyAsync(task, threadExecutor))
    );
  }

  @Override
  public long getRemainingCount() {
    return countDownLatch.getCount();
  }

  @Override
  public Optional<ReaderTaskResult> getWorkResult() {
    return futures.stream().map(CompletableFuture::join)
        .filter(Objects::nonNull)
        .map(f -> (ReaderTaskResult) f)
        .peek(f -> f.padding())
        .reduce(
            (r1, r2) -> {
              Map<String, Long> perf = Maps.newHashMap(r1.getPerf());
              if (r2.getSuccessCount() > 0) {
                perf.put(r2.getTableNameMapString(), r2.getRecordCount());
              }
              Map<String, Throwable> except = Maps.newHashMap(r1.getExcept());
              if (r2.getExcept().size() > 0) {
                except.putAll(r2.getExcept());
              }
              return ReaderTaskResult.builder()
                  .perf(perf)
                  .except(except)
                  .successCount(r1.getSuccessCount() + r2.getSuccessCount())
                  .failureCount(r1.getFailureCount() + r2.getFailureCount())
                  .recordCount(r1.getRecordCount() + r2.getRecordCount())
                  .totalBytes(r1.getTotalBytes() + r2.getTotalBytes())
                  .throwable(Objects.nonNull(r1.getThrowable()) ? r1.getThrowable() : r2.getThrowable())
                  .build();
            }
        );
  }

}
