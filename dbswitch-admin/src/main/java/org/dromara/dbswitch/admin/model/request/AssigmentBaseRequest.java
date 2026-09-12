// Copyright tang.  All rights reserved.
// https://gitee.com/inrgihc/dbswitch
//
// Use of this source code is governed by a BSD-style license
//
// Author: tang (inrgihc@126.com)
// Date : 2020/1/2
// Location: beijing , china
/////////////////////////////////////////////////////////////
package org.dromara.dbswitch.admin.model.request;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.dbswitch.admin.entity.AssignmentConfigEntity;
import org.dromara.dbswitch.admin.type.IncludeExcludeEnum;
import org.dromara.dbswitch.common.entity.PatternMapper;
import org.dromara.dbswitch.common.entity.TableColumnPair;
import org.dromara.dbswitch.common.type.CaseConvertEnum;
import org.dromara.dbswitch.common.type.ProductTableEnum;
import org.dromara.dbswitch.common.type.SyncOptionEnum;

public class AssigmentBaseRequest {

  @NoArgsConstructor
  @Data
  public static class AssigmentConfig {

    private Long sourceConnectionId;
    private String sourceSchema;
    private ProductTableEnum tableType;
    private IncludeExcludeEnum includeOrExclude;
    private List<String> sourceTables;
    private List<TableColumnPair> incrTableColumns;
    private String sourceBeforeSqlScripts;
    private String sourceAfterSqlScripts;

    private Long targetConnectionId;
    private String targetSchema;
    private CaseConvertEnum tableNameCase;
    private CaseConvertEnum columnNameCase;
    private List<PatternMapper> tableNameMapper;
    private List<PatternMapper> columnNameMapper;
    private Boolean targetDropTable;
    private Boolean targetOnlyCreate;
    private Boolean targetAlwaysDrop;
    private Boolean smartRecovery;
    private Boolean targetAutoIncrement;
    private SyncOptionEnum targetSyncOption;
    private String targetBeforeSqlScripts;
    private String targetAfterSqlScripts;
    private Integer batchSize;
    private Integer channelSize;
    private Map<String, String> customDdlMap;
  }

  protected AssignmentConfigEntity toAssignmentConfig(final Long assignmentId, final AssigmentConfig config) {
    AssignmentConfigEntity assignmentConfigEntity = new AssignmentConfigEntity();
    assignmentConfigEntity.setAssignmentId(assignmentId);
    assignmentConfigEntity.setSourceConnectionId(config.getSourceConnectionId());
    assignmentConfigEntity.setSourceSchema(config.getSourceSchema());
    assignmentConfigEntity.setTableType(config.getTableType());
    assignmentConfigEntity.setSourceTables(config.getSourceTables());
    assignmentConfigEntity.setIncrTableColumns(config.getIncrTableColumns());
    assignmentConfigEntity.setExcludedFlag(getExcludedFlag(config.getIncludeOrExclude()));
    assignmentConfigEntity.setPreSqlScripts(getTrimValueOrNull(config.getSourceBeforeSqlScripts()));
    assignmentConfigEntity.setPostSqlScripts(getTrimValueOrNull(config.getSourceAfterSqlScripts()));
    assignmentConfigEntity.setTargetConnectionId(config.getTargetConnectionId());
    assignmentConfigEntity.setTargetSchema(config.getTargetSchema());
    assignmentConfigEntity.setTableNameCase(config.getTableNameCase());
    assignmentConfigEntity.setColumnNameCase(config.getColumnNameCase());
    assignmentConfigEntity.setTableNameMap(config.getTableNameMapper());
    assignmentConfigEntity.setColumnNameMap(config.getColumnNameMapper());
    assignmentConfigEntity.setTargetDropTable(config.getTargetDropTable());
    assignmentConfigEntity.setTargetOnlyCreate(config.getTargetOnlyCreate());
    assignmentConfigEntity.setTargetAlwaysDrop(config.getTargetAlwaysDrop());
    assignmentConfigEntity.setSmartRecovery(config.getSmartRecovery());
    assignmentConfigEntity.setTargetAutoIncrement(config.getTargetAutoIncrement());
    assignmentConfigEntity.setBeforeSqlScripts(getTrimValueOrNull(config.getTargetBeforeSqlScripts()));
    assignmentConfigEntity.setAfterSqlScripts(getTrimValueOrNull(config.getTargetAfterSqlScripts()));
    assignmentConfigEntity.setTargetSyncOption(config.getTargetSyncOption());
    assignmentConfigEntity.setBatchSize(getValueOrDefault(config.getBatchSize(), 10000));
    assignmentConfigEntity.setChannelSize(getValueOrDefault(config.getChannelSize(), 100));
    assignmentConfigEntity.setCustomDdlMap(config.getCustomDdlMap());
    assignmentConfigEntity.setFirstFlag(Boolean.FALSE);

    return assignmentConfigEntity;
  }

  protected boolean getExcludedFlag(IncludeExcludeEnum includeOrExclude) {
    return includeOrExclude == IncludeExcludeEnum.EXCLUDE;
  }

  protected int getValueOrDefault(Integer value, int defaultValue) {
    return Objects.nonNull(value) ? value : defaultValue;
  }

  protected String getTrimValueOrNull(String value) {
    return Objects.nonNull(value) ? value.trim() : null;
  }
}
