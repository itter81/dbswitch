<template>
  <div class="edit-page">
    <el-card class="edit-card">
      <!-- 页面头部 -->
      <div class="edit-page-header">
        <div class="header-left">
          <el-button
            type="text"
            icon="el-icon-arrow-left"
            @click="handleCancel"
            class="back-btn">返回列表</el-button>
          <span class="header-divider">|</span>
          <h2 class="page-title">{{ isEditMode ? '编辑迁移任务' : '新建迁移任务' }}</h2>
          <el-tag v-if="dataform.name" size="small" type="info" style="margin-left:10px">{{ dataform.name }}</el-tag>
        </div>
        <div class="header-right">
          <span class="step-hint">步骤 {{ active }} / 5 · {{ stepTitles[active - 1] }}</span>
        </div>
      </div>

      <!-- 步骤条（支持点击已访问步骤） -->
      <div class="steps-wrapper">
        <el-steps :active="active" finish-status="success" align-center>
          <el-step
            title="基本信息"
            description="任务名称与调度方式"
            @click.native="goToStep(1)">
          </el-step>
          <el-step
            title="源端配置"
            description="数据源与表选择"
            @click.native="goToStep(2)">
          </el-step>
          <el-step
            title="目标端配置"
            description="目标库与同步方式"
            @click.native="goToStep(3)">
          </el-step>
          <el-step
            title="映射转换"
            description="表名/字段名映射规则"
            @click.native="goToStep(4)">
          </el-step>
          <el-step
            title="确认提交"
            description="检查配置并提交"
            @click.native="goToStep(5)">
          </el-step>
        </el-steps>
      </div>

      <el-form
        :model="dataform"
        status-icon
        :rules="rules"
        ref="dataform"
        label-width="160px"
        class="edit-form">

        <!-- ===== Step 1: 基本信息 ===== -->
        <div v-show="active == 1" class="step-content">
          <div class="step-section-title">
            <i class="el-icon-tickets"></i> 基本信息配置
          </div>
          <div class="form-area">
            <el-form-item label="任务名称" :required="true" prop="name">
              <el-input
                v-model="dataform.name"
                auto-complete="off"
                placeholder="请输入任务名称"
                clearable>
              </el-input>
              <div class="field-tips">只能以字母、数字开头，包含字母、数字和 . _ -，3~100 个字符</div>
            </el-form-item>
            <el-form-item label="任务描述" prop="description">
              <el-input
                v-model="dataform.description"
                type="textarea"
                :rows="3"
                auto-complete="off"
                placeholder="请输入任务描述（可选）">
              </el-input>
            </el-form-item>
            <el-form-item label="集成模式" :required="true" prop="scheduleMode">
              <el-input v-model="dataform.scheduleMode" v-if="false"></el-input>
              <el-radio-group
                v-model="dataform.scheduleModeName"
                size="small"
                @change="scheduleModeUpdate">
                <el-radio-button value="MANUAL" label="手动调度"></el-radio-button>
                <el-radio-button value="SYSTEM_SCHEDULED" label="系统调度"></el-radio-button>
              </el-radio-group>
              <div class="field-tips">手动调度：仅在手动触发时执行；系统调度：按 CRON 表达式定时触发</div>
            </el-form-item>
            <el-form-item
              label="执行周期"
              :required="true"
              v-if="dataform.scheduleMode == 'SYSTEM_SCHEDULED'">
              <el-select
                v-model="dataform.cronExpression"
                filterable
                allow-create
                placeholder="选择或输入 CRON 表达式"
                style="width:100%">
                <el-option
                  v-for="(item, index) in cronExprOptionList"
                  :key="index"
                  :label="item.name"
                  :value="item.value">
                </el-option>
              </el-select>
              <div class="field-tips">可选内置周期，也可自行输入 CRON 表达式（最小间隔 2 分钟）</div>
            </el-form-item>
          </div>
        </div>

        <!-- ===== Step 2: 源端配置 ===== -->
        <div v-show="active == 2" class="step-content">
          <div class="step-section-title">
            <i class="el-icon-upload2"></i> 同步源端配置
          </div>
          <div class="form-area">
            <el-form-item label="源端数据源" :required="true" prop="sourceConnectionId">
              <el-select
                v-model="dataform.sourceConnectionId"
                @change="selectChangedSourceConnection"
                placeholder="请选择源端数据源"
                style="width:100%">
                <el-option
                  v-for="(item, index) in connectionNameList"
                  :key="index"
                  :label="`[${item.id}] ${item.name}`"
                  :value="item.id">
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="源端模式名" :required="true" prop="sourceSchema">
              <el-select
                v-model="dataform.sourceSchema"
                filterable
                @change="selectCreateChangedSourceSchema"
                placeholder="请选择 Schema"
                style="width:100%">
                <el-option
                  v-for="(item, index) in sourceConnectionSchemas"
                  :key="index"
                  :label="item"
                  :value="item">
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="源端表类型" :required="true" prop="tableType">
              <el-select
                placeholder="请选择表类型"
                @change="selectCreateChangedTableType"
                v-model="dataform.tableType"
                style="width:100%">
                <el-option label="物理表" value="TABLE"></el-option>
                <el-option label="视图表" value="VIEW"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="配置方式" :required="true" prop="includeOrExclude">
              <el-select
                placeholder="请选择表选择方式"
                v-model="dataform.includeOrExclude"
                style="width:100%">
                <el-option label="包含表（指定要同步的表）" value="INCLUDE"></el-option>
                <el-option label="排除表（指定不同步的表）" value="EXCLUDE"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="表名配置" prop="sourceTables">
              <el-select
                placeholder="请选择表名（包含表可不选代表全部）"
                multiple
                filterable
                v-model="dataform.sourceTables"
                style="width:100%">
                <el-option
                  v-for="(item, index) in sourceSchemaTables"
                  :key="index"
                  :label="item"
                  :value="item">
                </el-option>
              </el-select>
              <div class="field-tips">
                包含表：选择要精确包含的表名，不选则代表全部；排除表：必须选择要排除的表名
              </div>
            </el-form-item>

            <!-- 增量同步配置 -->
            <el-form-item label="增量同步配置">
              <div class="incr-config-bar">
                <el-button
                  type="primary"
                  size="small"
                  icon="el-icon-plus"
                  plain
                  @click="handleAddInputIncrTable">
                  配置增量字段
                </el-button>
                <el-badge
                  v-if="dataform.incrTableColumns && dataform.incrTableColumns.length > 0"
                  :value="dataform.incrTableColumns.length"
                  type="warning"
                  style="margin-left:8px">
                </el-badge>
                <el-button
                  type="info"
                  size="small"
                  icon="el-icon-question"
                  plain
                  @click="showDataSyncMessageDialogVisible = true"
                  style="margin-left:8px">
                  同步说明
                </el-button>
              </div>
              <el-table
                v-if="dataform.incrTableColumns && dataform.incrTableColumns.length > 0"
                :data="dataform.incrTableColumns"
                :header-cell-style="{background:'#f5f7fa',color:'#606266'}"
                size="mini"
                border
                style="margin-top:10px">
                <el-table-column label="表名" prop="tableName" min-width="45%"></el-table-column>
                <el-table-column label="增量字段名" prop="columnName" min-width="45%"></el-table-column>
                <el-table-column label="操作" min-width="10%" align="center">
                  <template slot-scope="scope">
                    <el-button
                      type="danger"
                      size="mini"
                      icon="el-icon-delete"
                      circle
                      @click="handleDeleteIncrTable(scope.$index)">
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div class="field-tips">配置增量字段后，大表将通过增量字段条件查询来加快同步速度</div>
            </el-form-item>

            <!-- SQL 脚本：折叠面板 -->
            <el-form-item label="SQL 脚本（可选）">
              <el-collapse class="sql-collapse">
                <el-collapse-item title="展开配置同步前后置 SQL 脚本" name="sql">
                  <el-form-item label="同步前置 SQL" label-width="120px" prop="sourceBeforeSqlScripts" style="margin-bottom:12px">
                    <el-input
                      v-model="dataform.sourceBeforeSqlScripts"
                      type="textarea"
                      :rows="3"
                      placeholder="数据同步查询源端数据库前执行的 SQL，多个 SQL 间以英文逗号分隔">
                    </el-input>
                  </el-form-item>
                  <el-form-item label="同步后置 SQL" label-width="120px" prop="sourceAfterSqlScripts">
                    <el-input
                      v-model="dataform.sourceAfterSqlScripts"
                      type="textarea"
                      :rows="3"
                      placeholder="数据同步查询源端数据库后执行的 SQL，多个 SQL 间以英文逗号分隔">
                    </el-input>
                  </el-form-item>
                </el-collapse-item>
              </el-collapse>
            </el-form-item>
          </div>
        </div>

        <!-- ===== Step 3: 目标端配置 ===== -->
        <div v-show="active == 3" class="step-content">
          <div class="step-section-title">
            <i class="el-icon-download"></i> 目标端配置
          </div>
          <div class="form-area">
            <el-form-item label="目的端数据源" :required="true" prop="targetConnectionId">
              <el-select
                v-model="dataform.targetConnectionId"
                @change="selectChangedTargetConnection"
                placeholder="请选择目标端数据源"
                style="width:100%">
                <el-option
                  v-for="(item, index) in connectionNameList"
                  :key="index"
                  :label="`[${item.id}] ${item.name}`"
                  :value="item.id">
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="目的端模式名" :required="true" prop="targetSchema">
              <el-select
                v-model="dataform.targetSchema"
                filterable
                placeholder="请选择 Schema"
                style="width:100%">
                <el-option
                  v-for="(item, index) in targetConnectionSchemas"
                  :key="index"
                  :label="item"
                  :value="item">
                </el-option>
              </el-select>
            </el-form-item>

            <!-- 自动同步模式：卡片式选择 -->
            <el-form-item label="自动同步模式" label-width="160px" :required="true" prop="autoSyncMode">
              <div class="sync-mode-cards">
                <div
                  v-for="modeItem in syncModeOptions"
                  :key="modeItem.value"
                  class="sync-mode-card"
                  :class="{ 'is-selected': dataform.autoSyncMode === modeItem.value }"
                  @click="dataform.autoSyncMode = modeItem.value">
                  <div class="card-icon">
                    <i :class="modeItem.icon"></i>
                  </div>
                  <div class="card-body">
                    <div class="card-title">{{ modeItem.label }}</div>
                    <div class="card-desc">{{ modeItem.desc }}</div>
                  </div>
                  <div class="card-check" v-if="dataform.autoSyncMode === modeItem.value">
                    <i class="el-icon-check"></i>
                  </div>
                </div>
              </div>
            </el-form-item>

            <el-form-item
              label="建表字段自增"
              :required="true"
              v-if="dataform.autoSyncMode !== 0"
              prop="targetAutoIncrement">
              <el-select v-model="dataform.targetAutoIncrement" style="width:100%">
                <el-option label="是" :value="true"></el-option>
                <el-option label="否" :value="false"></el-option>
              </el-select>
              <div class="field-tips">创建表时是否自动支持字段的自增，只对自动建表时生效</div>
            </el-form-item>

            <!-- 高级选项折叠 -->
            <el-form-item label-width="0">
              <el-collapse class="advanced-collapse">
                <el-collapse-item name="advanced">
                  <template slot="title">
                    <span class="advanced-title"><i class="el-icon-setting"></i> 高级选项（名称转换、批次大小等）</span>
                  </template>

                  <el-form-item label="表名转换方法" label-width="160px" :required="true" prop="tableNameCase">
                    <el-select v-model="dataform.tableNameCase" style="width:100%">
                      <el-option
                        v-for="(item, index) in nameConvertList"
                        :key="index"
                        :label="item.name"
                        :value="item.value">
                      </el-option>
                    </el-select>
                    <div class="field-tips">先使用表名映射，再使用此方法转换，对大小写敏感的数据库有效</div>
                  </el-form-item>

                  <el-form-item label="列名转换方法" label-width="160px" :required="true" prop="columnNameCase">
                    <el-select v-model="dataform.columnNameCase" style="width:100%">
                      <el-option
                        v-for="(item, index) in nameConvertList"
                        :key="index"
                        :label="item.name"
                        :value="item.value">
                      </el-option>
                    </el-select>
                    <div class="field-tips">先使用列名映射，再使用此方法转换，对大小写敏感的数据库有效</div>
                  </el-form-item>

                  <template v-if="dataform.autoSyncMode !== 1">
                    <el-form-item label="数据批次大小" label-width="160px" :required="true" prop="batchSize">
                      <el-select v-model="dataform.batchSize" style="width:100%">
                        <el-option
                          v-for="(item, index) in batchSizeList"
                          :key="index"
                          :label="item.toString()"
                          :value="item">
                        </el-option>
                      </el-select>
                      <div class="field-tips">单批次处理行数，越大越占内存。小字段表建议 10000，大字段表建议 100~500</div>
                    </el-form-item>
                    <el-form-item label="通道队列大小" label-width="160px" :required="true" prop="channelSize">
                      <el-select v-model="dataform.channelSize" style="width:100%">
                        <el-option
                          v-for="(item, index) in channelSizeList"
                          :key="index"
                          :label="item.toString()"
                          :value="item">
                        </el-option>
                      </el-select>
                      <div class="field-tips">数据缓冲通道大小，最大内存 = 行大小 × 批次大小 × 通道大小</div>
                    </el-form-item>
                    <el-form-item label="同步操作方法" label-width="160px" :required="true" prop="targetSyncOption">
                      <el-select v-model="dataform.targetSyncOption" style="width:100%">
                        <el-option
                          v-for="(item, index) in targetSyncOptionList"
                          :key="index"
                          :label="item.name"
                          :value="item.value">
                        </el-option>
                      </el-select>
                      <div class="field-tips">配置变化量同步时执行 INSERT / UPDATE / DELETE 操作的方法，仅对有主键表有效</div>
                    </el-form-item>

                    <el-form-item label="同步前置 SQL" label-width="160px" prop="targetBeforeSqlScripts">
                      <el-input
                        v-model="dataform.targetBeforeSqlScripts"
                        type="textarea"
                        :rows="3"
                        placeholder="写入目标端数据库前执行的 SQL，多个 SQL 以英文逗号分隔">
                      </el-input>
                    </el-form-item>
                    <el-form-item label="同步后置 SQL" label-width="160px" prop="targetAfterSqlScripts">
                      <el-input
                        v-model="dataform.targetAfterSqlScripts"
                        type="textarea"
                        :rows="3"
                        placeholder="写入目标端数据库后执行的 SQL，多个 SQL 以英文逗号分隔">
                      </el-input>
                    </el-form-item>
                  </template>
                </el-collapse-item>
              </el-collapse>
            </el-form-item>
          </div>
        </div>

        <!-- ===== Step 4: 映射转换配置 ===== -->
        <div v-show="active == 4" class="step-content">
          <div class="step-section-title">
            <i class="el-icon-sort"></i> 映射转换配置
          </div>
          <el-alert
            title="映射规则说明"
            type="info"
            :closable="false"
            style="margin-bottom:16px">
            <div slot="default">
              <span>① 表名映射为空时，目标表名与源表名相同；</span>
              <span>② 字段名映射为空时，目标字段名与源字段名相同；</span>
              <span>③ 若目标字段名填写为空，则该字段不参与同步（非主键）。</span>
            </div>
          </el-alert>

          <el-tabs type="border-card" class="mapper-tabs">
            <el-tab-pane>
              <span slot="label"><i class="el-icon-s-grid"></i> 表名映射（{{ dataform.tableNameMapper.length }}条）</span>
              <div class="tab-action-bar">
                <el-button type="primary" size="small" icon="el-icon-plus" @click="addTableNameMapperListRow()">
                  添加表名映射
                </el-button>
                <el-button type="success" size="small" icon="el-icon-view" @click="previewTableNameMapList()">
                  预览映射结果
                </el-button>
              </div>
              <el-table
                :data="dataform.tableNameMapper"
                size="small"
                border
                style="margin-top:10px">
                <template slot="empty">
                  <span class="table-empty-text">暂无表名映射规则，表名将保持与源端一致</span>
                </template>
                <el-table-column label="源端表名（正则匹配）" min-width="45%">
                  <template slot-scope="scope">
                    <el-input v-model="scope.row.fromPattern" placeholder="填写正则表达式"></el-input>
                  </template>
                </el-table-column>
                <el-table-column label="目标端替换值" min-width="45%">
                  <template slot-scope="scope">
                    <el-input v-model="scope.row.toValue" placeholder="填写替换后的目标值"></el-input>
                  </template>
                </el-table-column>
                <el-table-column label="操作" min-width="10%" align="center">
                  <template slot-scope="scope">
                    <el-button
                      size="mini"
                      type="danger"
                      icon="el-icon-delete"
                      circle
                      @click="deleteTableNameMapperListItem(scope.$index)">
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane>
              <span slot="label"><i class="el-icon-document-copy"></i> 字段名映射（{{ dataform.columnNameMapper.length }}条）</span>
              <div class="tab-action-bar">
                <el-button type="primary" size="small" icon="el-icon-plus" @click="addColumnNameMapperListRow()">
                  添加字段名映射
                </el-button>
                <el-button type="success" size="small" icon="el-icon-view" @click="previewColumnNameMapList()">
                  预览映射结果
                </el-button>
              </div>
              <el-table
                :data="dataform.columnNameMapper"
                size="small"
                border
                style="margin-top:10px">
                <template slot="empty">
                  <span class="table-empty-text">暂无字段名映射规则，字段名将保持与源端一致</span>
                </template>
                <el-table-column label="源端字段名（正则匹配）" min-width="45%">
                  <template slot-scope="scope">
                    <el-input v-model="scope.row.fromPattern" placeholder="填写正则表达式"></el-input>
                  </template>
                </el-table-column>
                <el-table-column label="目标端替换值（空=剔除该字段）" min-width="45%">
                  <template slot-scope="scope">
                    <el-input v-model="scope.row.toValue" placeholder="为空时该字段不参与同步"></el-input>
                  </template>
                </el-table-column>
                <el-table-column label="操作" min-width="10%" align="center">
                  <template slot-scope="scope">
                    <el-button
                      size="mini"
                      type="danger"
                      icon="el-icon-delete"
                      circle
                      @click="deleteColumnNameMapperListItem(scope.$index)">
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>

          <!-- DDL预览/编辑入口 -->
          <div v-if="dataform.autoSyncMode !== 0" style="margin-top:16px">
            <el-divider content-position="left">建表语句预览/编辑</el-divider>
            <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap">
              <el-button
                type="primary"
                icon="el-icon-document"
                :disabled="!canPreviewDdl"
                @click="handlePreviewDdl">
                预览/编辑建表语句
              </el-button>
              <el-tag
                v-if="customDdlModifiedCount > 0"
                type="warning"
                size="small"
                effect="dark">
                已编辑 {{ customDdlModifiedCount }} 张表的建表语句
              </el-tag>
            </div>
            <div class="field-tips" v-if="!canPreviewDdl">
              请先选择【源端数据源】【源端模式名】【目的端数据源】【目的端模式名】和【配置方式】，方可使用此功能
            </div>
            <div class="field-tips" v-else>
              查看和编辑系统为每张目标表自动生成的 CREATE TABLE 建表语句，适用于需要调整字段类型、添加表属性等场景
            </div>
          </div>
        </div>

        <!-- ===== Step 5: 确认提交 ===== -->
        <div v-show="active == 5" class="step-content">
          <div class="step-section-title">
            <i class="el-icon-finished"></i> 配置确认
          </div>
          <el-alert
            title="请确认以下配置信息无误后再提交"
            type="success"
            :closable="false"
            show-icon
            style="margin-bottom:16px">
          </el-alert>
          <commonInfo :infoform="dataform"></commonInfo>
        </div>
      </el-form>

      <!-- 底部操作栏 -->
      <div class="footer-bar">
        <el-button
          icon="el-icon-close"
          @click="handleCancel">
          取消返回
        </el-button>
        <div class="footer-right">
          <el-button
            icon="el-icon-arrow-left"
            v-if="active > 1"
            @click="pre">
            上一步
          </el-button>
          <el-button
            type="primary"
            icon="el-icon-arrow-right"
            v-if="active > 0 && active < 5"
            @click="next">
            下一步
          </el-button>
          <el-button
            type="primary"
            icon="el-icon-check"
            v-if="active == 5"
            @click="handleSave">
            {{ isEditMode ? '保存修改' : '提交创建' }}
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- ===== 弹窗：增量同步字段选择 ===== -->
    <el-dialog
      v-if="active == 2"
      title="选择增量同步表的增量标识字段"
      :visible.sync="columnNameIncrementDialogVisible"
      :showClose="false"
      :before-close="handleClose"
      width="680px">
      <el-select
        @change="queryPreviewColumnNameMapperList"
        v-model="preiveTableName"
        placeholder="请先选择表名"
        style="margin-bottom:12px;width:100%">
        <el-option
          v-for="(item, index) in preiveSeeTableNameList"
          :key="index"
          :label="item"
          :value="item">
        </el-option>
      </el-select>
      <el-table
        :header-cell-style="{background:'#f5f7fa',color:'#606266'}"
        :data="columnNamesMapperData"
        @row-click="singleRowClick"
        highlight-current-row
        size="mini"
        border>
        <el-table-column label="#" min-width="10%">
          <template slot-scope="scope">{{ scope.$index }}</template>
        </el-table-column>
        <el-table-column prop="originalName" label="字段名" min-width="30%"></el-table-column>
        <el-table-column prop="typeName" label="字段类型" min-width="30%"></el-table-column>
        <el-table-column prop="canIncrement" label="可标识增量" min-width="20%">
          <template slot-scope="scope">
            <el-tag :type="scope.row.canIncrement ? 'success' : 'info'" size="mini">
              {{ boolValueFormat(scope.row.canIncrement) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="选择" min-width="10%">
          <template slot-scope="scope">
            <el-radio
              :label="scope.row.originalName"
              v-model="radio"
              :disabled="!scope.row.canIncrement"
              @change.native="singleRowClick(scope.row)">
              {{ "" }}
            </el-radio>
          </template>
        </el-table-column>
      </el-table>
      <div slot="footer" class="dialog-footer">
        <el-button @click="handleCancelSelectIncrTableColumn">取消</el-button>
        <el-button type="primary" @click="handleConfirmSelectIncrTableColumn">确定</el-button>
      </div>
    </el-dialog>

    <!-- ===== 弹窗：同步说明 ===== -->
    <el-dialog
      v-if="active == 2"
      title="数据同步说明"
      :visible.sync="showDataSyncMessageDialogVisible"
      :showClose="false"
      :before-close="handleClose"
      width="640px">
      <el-alert title="同步模式说明" type="warning" :closable="false" show-icon>
        <ul style="padding-left:16px;margin:8px 0">
          <li><b>全量同步：</b>先 truncate 清空目标表，再将源端数据全部插入目标表</li>
          <li><b>增量同步：</b>根据增量字段使用带 WHERE 条件查询源端，然后插入目标表</li>
          <li><b>变化量同步：</b>两端主键一致时，通过数据比对计算差异，执行增删改操作</li>
        </ul>
      </el-alert>
      <el-alert title="dbswitch 同步执行逻辑" type="info" :closable="false" show-icon style="margin-top:10px">
        <ol style="padding-left:16px;margin:8px 0">
          <li>首次同步：自动建目标表，执行全量数据同步</li>
          <li>非首次且配置了增量字段：执行增量数据同步</li>
          <li>非首次且无增量字段，两端主键一致：执行变化量数据同步</li>
          <li>非首次且无增量字段，无主键或主键不一致：执行全量数据同步</li>
        </ol>
      </el-alert>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="showDataSyncMessageDialogVisible = false">知道了</el-button>
      </div>
    </el-dialog>

    <!-- ===== 弹窗：预览表名映射 ===== -->
    <el-dialog
      v-if="active == 4"
      title="预览表名映射结果"
      :visible.sync="tableNameMapperDialogVisible"
      :showClose="false"
      :before-close="handleClose">
      <el-table
        :header-cell-style="{background:'#f5f7fa',color:'#606266'}"
        :data="tableNamesMapperData"
        size="small"
        border>
        <el-table-column prop="originalName" label="源端表名" min-width="50%"></el-table-column>
        <el-table-column prop="targetName" label="目标表名" min-width="50%"></el-table-column>
      </el-table>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="tableNameMapperDialogVisible = false">关闭</el-button>
      </div>
    </el-dialog>

    <!-- ===== 弹窗：预览字段映射 ===== -->
    <el-dialog
      v-if="active == 4"
      title="预览字段名映射结果"
      :visible.sync="columnNameMapperDialogVisible"
      :showClose="false"
      :before-close="handleClose">
      <el-select
        @change="queryPreviewColumnNameMapperList"
        v-model="preiveTableName"
        placeholder="请选择表名"
        style="margin-bottom:12px;width:100%">
        <el-option
          v-for="(item, index) in preiveSeeTableNameList"
          :key="index"
          :label="item"
          :value="item">
        </el-option>
      </el-select>
      <el-table
        :header-cell-style="{background:'#f5f7fa',color:'#606266'}"
        :data="columnNamesMapperData"
        size="small"
        border>
        <el-table-column prop="originalName" label="源端字段名" min-width="50%"></el-table-column>
        <el-table-column prop="targetName" label="目标端字段名" min-width="50%"></el-table-column>
      </el-table>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="columnNameMapperDialogVisible = false">关闭</el-button>
      </div>
    </el-dialog>

    <!-- DDL预览编辑对话框 -->
    <ddl-preview-dialog
      :dialog-visible.sync="ddlPreviewDialogVisible"
      :preview-request-params="ddlPreviewRequestParams"
      @confirm="handleDdlConfirm"
      ref="ddlPreviewDialogRef"
    />
  </div>
</template>

<script>
import commonInfo from '@/views/task/common/info'
import ddlPreviewDialog from '@/views/task/common/ddl-preview-dialog'

export default {
  components: { commonInfo, ddlPreviewDialog },
  data () {
    return {
      stepTitles: ['基本信息配置', '同步源端配置', '目标端配置', '映射转换配置', '配置确认提交'],
      syncModeOptions: [
        {
          value: 2,
          label: '目标端建表并同步数据',
          desc: '首次自动建表（存在同名表时删除重建），并执行全量同步；再次执行时根据主键进行变化量同步',
          icon: 'el-icon-refresh'
        },
        {
          value: 1,
          label: '目标端只创建物理表',
          desc: '每次执行时只在目标端自动建表，存在同名表时删除重建，不执行数据同步',
          icon: 'el-icon-files'
        },
        {
          value: 0,
          label: '目标端只同步表里数据',
          desc: '目标端需已存在符合映射规则的表，适用于两端表结构一致时的数据同步场景',
          icon: 'el-icon-s-order'
        }
      ],
      cronExprOptionList: [
        { name: "每1分钟执行1次", value: "0 0/1 * * * ? *" },
        { name: "每2分钟执行1次", value: "0 0/2 * * * ? *" },
        { name: "每3分钟执行1次", value: "0 0/3 * * * ? *" },
        { name: "每4分钟执行1次", value: "0 0/4 * * * ? *" },
        { name: "每5分钟执行1次", value: "0 0/5 * * * ? *" },
        { name: "每30分钟执行1次", value: "0 0/30 * * * ? *" },
        { name: "每1小时执行1次", value: "0 0 0/1 * * ? *" },
        { name: "每2小时执行1次", value: "0 0 0/2 * * ? *" },
        { name: "每8小时执行1次", value: "0 0 0/8 * * ? *" },
        { name: "每12小时执行1次", value: "0 0 0/12 * * ? *" },
        { name: "每日0时执行1次", value: "0 0 0 1/1 * ? *" }
      ],
      nameConvertList: [
        { name: "无转换", value: "NONE" },
        { name: "转大写", value: "UPPER" },
        { name: "转小写", value: "LOWER" },
        { name: "下划线转驼峰", value: "CAMEL" },
        { name: "驼峰转下划线", value: "SNAKE" }
      ],
      batchSizeList: [100, 500, 1000, 5000, 10000, 20000],
      channelSizeList: [10, 20, 40, 60, 80, 100, 200, 500, 1000],
      targetSyncOptionList: [
        { name: "只同步INSERT操作", value: "ONLY_INSERT" },
        { name: "只同步UPDATE操作", value: "ONLY_UPDATE" },
        { name: "只同步INSERT和UPDATE", value: "INSERT_UPDATE" },
        { name: "只同步DELETE操作", value: "ONLY_DELETE" },
        { name: "只同步UPDATE和DELETE", value: "UPDATE_DELETE" },
        { name: "执行所有的同步操作", value: "INSERT_UPDATE_DELETE" }
      ],
      connectionNameList: [],
      dataform: {
        id: 0,
        name: "",
        description: "",
        scheduleMode: "MANUAL",
        scheduleModeName: "手动调度",
        cronExpression: "",
        sourceConnectionId: '请选择',
        sourceTypeName: 'MySQL',
        sourceSchema: "",
        tableType: "TABLE",
        includeOrExclude: "",
        sourceTables: [],
        incrTableColumns: [],
        sourceBeforeSqlScripts: "",
        sourceAfterSqlScripts: "",
        tableNameMapper: [],
        columnNameMapper: [],
        tableNameCase: 'NONE',
        columnNameCase: 'NONE',
        targetConnectionId: '请选择',
        targetTypeName: 'MySQL',
        targetDropTable: true,
        targetOnlyCreate: false,
        targetAutoIncrement: false,
        autoSyncMode: 2,
        targetSchema: "",
        batchSize: 5000,
        channelSize: 100,
        targetSyncOption: 'INSERT_UPDATE_DELETE',
        targetBeforeSqlScripts: '',
        targetAfterSqlScripts: '',
        customDdlMap: {},
      },
      rules: {
        name: [{ required: true, message: "任务名称不能为空", trigger: "blur" }],
        scheduleMode: [{ required: true, type: 'string', message: "调度方式必须选择", trigger: "change" }],
        sourceConnectionId: [{ required: true, type: 'integer', message: "必须选择一个来源端", trigger: "change" }],
        sourceSchema: [{ required: true, type: 'string', message: "必须选择一个Schema名", trigger: "change" }],
        tableType: [{ required: true, type: 'string', message: "表类型必须选择", trigger: "change" }],
        includeOrExclude: [{ required: true, type: 'string', message: "配置方式必须选择", trigger: "change" }],
        sourceTables: [{ required: false, type: 'array', message: "必须选择一个Table名", trigger: "change" }],
        targetConnectionId: [{ required: true, type: 'integer', message: "必须选择一个目的端", trigger: "change" }],
        targetSchema: [{ required: true, type: 'string', message: "必须选择一个Schema名", trigger: "change" }],
        batchSize: [{ required: true, type: 'integer', message: "必须选择数据批次大小", trigger: "change" }],
        channelSize: [{ required: true, type: 'integer', message: "必须选择通道队列大小", trigger: "change" }],
        targetSyncOption: [{ required: true, type: 'string', message: "必须选择同步方法", trigger: "change" }]
      },
      active: 1,
      maxVisitedStep: 1,
      radio: '0',
      sourceConnection: {},
      targetConnection: {},
      sourceConnectionSchemas: [],
      sourceSchemaTables: [],
      targetConnectionSchemas: [],
      columnNameIncrementDialogVisible: false,
      showDataSyncMessageDialogVisible: false,
      tableNameMapperDialogVisible: false,
      columnNameMapperDialogVisible: false,
      tableNamesMapperData: [],
      columnNamesMapperData: [],
      preiveSeeTableNameList: [],
      preiveTableName: "",
      tempIncrTableName: "",
      tempIncrColumnName: "",
      ddlPreviewDialogVisible: false,
      customDdlModifiedCount: 0,
      ddlPreviewRequestParams: {},
    }
  },
  computed: {
    isEditMode () {
      return !!(this.$route.query.id && this.$route.query.id > 0)
    },
    canPreviewDdl: function () {
      return this.dataform.sourceConnectionId > 0
        && this.dataform.sourceSchema
        && this.dataform.targetConnectionId > 0
        && this.dataform.targetSchema
        && !!this.dataform.includeOrExclude
    }
  },
  methods: {
    goToStep (step) {
      // 只允许跳转到已访问过的步骤（或当前步骤）
      if (step <= this.maxVisitedStep) {
        this.active = step;
      }
    },
    initScheduleModeTemp (val) {
      if (val === 'SYSTEM_SCHEDULED') return "系统调度";
      if (val === 'MANUAL') return "手动调度";
    },
    scheduleModeUpdate (val) {
      if (val === '系统调度') this.dataform.scheduleMode = "SYSTEM_SCHEDULED";
      if (val === '手动调度') this.dataform.scheduleMode = "MANUAL";
    },
    boolValueFormat (value) {
      return value === true ? "是" : "否";
    },
    handleClose (done) {},
    handleCancel () {
      this.$router.push('/task/list');
    },
    next () {
      if (this.active < 5) {
        this.active++;
        if (this.active > this.maxVisitedStep) {
          this.maxVisitedStep = this.active;
        }
      }
    },
    pre () {
      if (this.active > 1) this.active--;
    },
    loadConnections: function () {
      this.connectionNameList = [];
      this.$http({
        method: "GET",
        url: "/dbswitch/admin/api/v1/connection/list/name"
      }).then(res => {
        if (0 === res.data.code) {
          this.connectionNameList = res.data.data;
        } else {
          this.connectionNameList = [];
          if (res.data.message) this.$message.error("加载连接列表失败：" + res.data.message);
        }
      });
    },
    loadAssignmentDetail: function () {
      if (this.$route.query.id && this.$route.query.id > 0) {
        this.$http.get(
          "/dbswitch/admin/api/v1/assignment/detail/id/" + this.$route.query.id
        ).then(res => {
          if (0 === res.data.code) {
            let detail = res.data.data;
            let varAutoSyncMode = 2;
            if (detail.configuration.targetDropTable && detail.configuration.targetOnlyCreate) {
              varAutoSyncMode = 1;
            } else if (!detail.configuration.targetDropTable && !detail.configuration.targetOnlyCreate) {
              varAutoSyncMode = 0;
            } else {
              varAutoSyncMode = 2;
            }
            this.dataform = {
              id: detail.id,
              name: detail.name,
              description: detail.description,
              scheduleMode: detail.scheduleMode,
              scheduleModeName: this.initScheduleModeTemp(detail.scheduleMode),
              cronExpression: detail.cronExpression,
              sourceConnectionId: detail.configuration.sourceConnectionId,
              sourceTypeName: detail.configuration.sourceTypeName,
              sourceConnectionName: detail.configuration.sourceConnectionName,
              sourceSchema: detail.configuration.sourceSchema,
              tableType: detail.configuration.tableType,
              includeOrExclude: detail.configuration.includeOrExclude,
              sourceTables: detail.configuration.sourceTables,
              incrTableColumns: detail.configuration.incrTableColumns,
              sourceBeforeSqlScripts: detail.configuration.sourceBeforeSqlScripts,
              sourceAfterSqlScripts: detail.configuration.sourceAfterSqlScripts,
              tableNameMapper: detail.configuration.tableNameMapper,
              columnNameMapper: detail.configuration.columnNameMapper,
              tableNameCase: detail.configuration.tableNameCase,
              columnNameCase: detail.configuration.columnNameCase,
              targetConnectionId: detail.configuration.targetConnectionId,
              targetTypeName: detail.configuration.targetTypeName,
              targetConnectionName: detail.configuration.targetConnectionName,
              targetDropTable: detail.configuration.targetDropTable,
              targetOnlyCreate: detail.configuration.targetOnlyCreate,
              targetAutoIncrement: detail.configuration.targetAutoIncrement,
              autoSyncMode: varAutoSyncMode,
              targetSchema: detail.configuration.targetSchema,
              batchSize: detail.configuration.batchSize,
              channelSize: detail.configuration.channelSize,
              targetSyncOption: detail.configuration.targetSyncOption,
              targetBeforeSqlScripts: detail.configuration.targetBeforeSqlScripts,
              targetAfterSqlScripts: detail.configuration.targetAfterSqlScripts,
              customDdlMap: detail.configuration.customDdlMap || {},
            };
            this.selectChangedSourceConnection(this.dataform.sourceConnectionId).then(() => {
              this.selectCreateChangedSourceSchema(this.dataform.sourceSchema);
            });
            this.selectChangedTargetConnection(this.dataform.targetConnectionId);
            // 编辑模式解锁所有步骤
            this.maxVisitedStep = 5;
          } else {
            if (res.data.message) this.$message.error("查询任务失败：" + res.data.message);
          }
        });
      }
    },
    selectChangedSourceConnection: function (value) {
      this.sourceConnection = this.connectionNameList.find(item => item.id === value);
      if (this.sourceConnection) this.dataform.sourceTypeName = this.sourceConnection.typeName;
      this.sourceConnectionSchemas = [];
      return this.$http.get("/dbswitch/admin/api/v1/connection/schemas/get/" + value).then(res => {
        if (0 === res.data.code) {
          this.sourceConnectionSchemas = res.data.data;
        } else {
          this.$message.error("查询来源端数据库的Schema失败," + res.data.message);
          this.sourceConnectionSchemas = [];
        }
      });
    },
    selectCreateChangedSourceSchema: function (value) {
      this.sourceSchemaTables = [];
      const url = 'TABLE' === this.dataform.tableType
        ? "/dbswitch/admin/api/v1/connection/tables/get/" + this.dataform.sourceConnectionId + "?schema=" + value
        : "/dbswitch/admin/api/v1/connection/views/get/" + this.dataform.sourceConnectionId + "?schema=" + value;
      this.$http.get(url).then(res => {
        if (0 === res.data.code) {
          this.sourceSchemaTables = res.data.data;
        } else {
          this.$message.error("查询来源端数据库表列表失败," + res.data.message);
          this.sourceSchemaTables = [];
        }
      });
    },
    selectCreateChangedTableType: function (value) {
      this.sourceSchemaTables = [];
      const url = 'TABLE' === value
        ? "/dbswitch/admin/api/v1/connection/tables/get/" + this.dataform.sourceConnectionId + "?schema=" + this.dataform.sourceSchema
        : "/dbswitch/admin/api/v1/connection/views/get/" + this.dataform.sourceConnectionId + "?schema=" + this.dataform.sourceSchema;
      this.$http.get(url).then(res => {
        if (0 === res.data.code) {
          this.sourceSchemaTables = res.data.data;
        } else {
          this.$message.error("查询来源端数据库表列表失败," + res.data.message);
          this.sourceSchemaTables = [];
        }
      });
    },
    handleAddInputIncrTable: function () {
      if (!this.dataform.sourceConnectionId || this.dataform.sourceConnectionId < 0
        || !this.dataform.sourceSchema || this.dataform.sourceSchema.length == 0) {
        this.$message.warning("请先选择【源端数据源】和【源端模式名】！");
        return;
      }
      if (!this.dataform.includeOrExclude) {
        this.$message.warning("请先选择源端表选择的【配置方式】！");
        return;
      }
      if (this.dataform.includeOrExclude == "INCLUDE") {
        this.preiveSeeTableNameList = this.dataform.sourceTables.length == 0
          ? this.sourceSchemaTables
          : this.dataform.sourceTables;
      } else {
        if (this.dataform.sourceTables.length == 0) {
          this.$message.warning("请先选择排除表的【表名配置】！");
          return;
        }
        this.preiveSeeTableNameList = JSON.parse(JSON.stringify(this.sourceSchemaTables));
        for (var i = 0; i < this.dataform.sourceTables.length; ++i) {
          var one = this.dataform.sourceTables[i];
          this.preiveSeeTableNameList.some((item, index) => {
            if (item == one) { this.preiveSeeTableNameList.splice(index, 1); return true; }
          });
        }
      }
      this.columnNameIncrementDialogVisible = true;
    },
    handleDeleteIncrTable: function (index) {
      this.dataform.incrTableColumns.splice(index, 1);
    },
    selectChangedTargetConnection: function (value) {
      this.targetConnection = this.connectionNameList.find(item => item.id === value);
      if (this.targetConnection) this.dataform.targetTypeName = this.targetConnection.typeName;
      this.targetConnectionSchemas = [];
      this.$http.get("/dbswitch/admin/api/v1/connection/schemas/get/" + value).then(res => {
        if (0 === res.data.code) {
          this.targetConnectionSchemas = res.data.data;
        } else {
          this.$message.error("查询目的端数据库的Schema失败," + res.data.message);
          this.targetConnectionSchemas = [];
        }
      });
    },
    addTableNameMapperListRow: function () {
      this.dataform.tableNameMapper.push({ "fromPattern": "", "toValue": "" });
    },
    deleteTableNameMapperListItem: function (index) {
      this.dataform.tableNameMapper.splice(index, 1);
    },
    previewTableNameMapList: function () {
      if (!this.dataform.sourceConnectionId || this.dataform.sourceConnectionId < 0
        || !this.dataform.sourceSchema || this.dataform.sourceSchema.length == 0) {
        this.$message.warning("请先选择【源端数据源】和【源端模式名】！");
        return;
      }
      if (!this.dataform.includeOrExclude) {
        this.$message.warning("请先选择源端表选择的【配置方式】！");
        return;
      }
      this.$http({
        method: "POST",
        headers: { 'Content-Type': 'application/json' },
        url: "/dbswitch/admin/api/v1/mapper/preview/table",
        data: JSON.stringify({
          id: this.dataform.sourceConnectionId,
          schemaName: this.dataform.sourceSchema,
          isInclude: this.dataform.includeOrExclude == 'INCLUDE',
          tableNames: this.dataform.sourceTables,
          nameMapper: this.dataform.tableNameMapper,
          tableNameCase: this.dataform.tableNameCase
        })
      }).then(res => {
        if (0 === res.data.code) {
          this.tableNamesMapperData = res.data.data;
          this.tableNameMapperDialogVisible = true;
        } else {
          this.tableNamesMapperData = [];
          if (res.data.message) this.$message.error(res.data.message);
        }
      });
    },
    addColumnNameMapperListRow: function () {
      this.dataform.columnNameMapper.push({ "fromPattern": "", "toValue": "" });
    },
    deleteColumnNameMapperListItem: function (index) {
      this.dataform.columnNameMapper.splice(index, 1);
    },
    previewColumnNameMapList: function () {
      if (!this.dataform.sourceConnectionId || this.dataform.sourceConnectionId <= 0
        || !this.dataform.sourceSchema || this.dataform.sourceSchema.length == 0) {
        this.$message.warning("请先选择【源端数据源】和【源端模式名】！");
        return;
      }
      if (!this.dataform.includeOrExclude) {
        this.$message.warning("请先选择源端表选择的【配置方式】！");
        return;
      }
      if (this.dataform.includeOrExclude == "INCLUDE") {
        this.preiveSeeTableNameList = this.dataform.sourceTables.length == 0
          ? this.sourceSchemaTables
          : this.dataform.sourceTables;
      } else {
        if (this.dataform.sourceTables.length == 0) {
          this.$message.warning("请先选择排除表的【表名配置】！");
          return;
        }
        this.preiveSeeTableNameList = JSON.parse(JSON.stringify(this.sourceSchemaTables));
        for (var i = 0; i < this.dataform.sourceTables.length; ++i) {
          var one = this.dataform.sourceTables[i];
          this.preiveSeeTableNameList.some((item, index) => {
            if (item == one) { this.preiveSeeTableNameList.splice(index, 1); return true; }
          });
        }
      }
      this.preiveTableName = "";
      this.columnNamesMapperData = [];
      this.columnNameMapperDialogVisible = true;
    },
    queryPreviewColumnNameMapperList: function () {
      if (!this.preiveSeeTableNameList || this.preiveSeeTableNameList.length == 0) {
        this.$message.warning("请在源端配置【表名配置】！");
        return;
      }
      if (!this.preiveTableName || this.preiveTableName.length == 0) {
        this.$message.warning("请选择一个表名！");
        return;
      }
      this.$http({
        method: "POST",
        headers: { 'Content-Type': 'application/json' },
        url: "/dbswitch/admin/api/v1/mapper/preview/column",
        data: JSON.stringify({
          id: this.dataform.sourceConnectionId,
          schemaName: this.dataform.sourceSchema,
          isInclude: this.dataform.includeOrExclude == 'INCLUDE',
          tableName: this.preiveTableName,
          nameMapper: this.dataform.columnNameMapper,
          columnNameCase: this.dataform.columnNameCase
        })
      }).then(res => {
        if (0 === res.data.code) {
          this.columnNamesMapperData = res.data.data;
        } else {
          if (res.data.message) this.$message.error(res.data.message);
        }
      });
    },
    singleRowClick (row) {
      if (row.canIncrement) {
        this.tempIncrTableName = this.preiveTableName;
        this.tempIncrColumnName = row.originalName;
        this.radio = row.originalName;
      } else {
        this.$alert("非整型或日期时间类型不能被选中", "提示信息", {
          confirmButtonText: "确定",
          type: "warn"
        });
      }
    },
    handleConfirmSelectIncrTableColumn: function () {
      if (!this.tempIncrTableName || !this.tempIncrColumnName) {
        this.$alert("请选择一个标识增量字段", "错误信息", {
          confirmButtonText: "确定",
          type: "error"
        });
        return;
      }
      if (!this.dataform.incrTableColumns.find(item => item.tableName === this.tempIncrTableName)) {
        this.dataform.incrTableColumns.push({
          tableName: this.tempIncrTableName,
          columnName: this.tempIncrColumnName
        });
        this.handleCancelSelectIncrTableColumn();
      } else {
        this.$alert("已经存在增量同步表[" + this.tempIncrTableName + "]的配置了", "提示信息", {
          confirmButtonText: "确定",
          type: "info"
        });
      }
    },
    handleCancelSelectIncrTableColumn: function () {
      this.columnNameIncrementDialogVisible = false;
      this.preiveTableName = "";
      this.columnNamesMapperData = [];
      this.tempIncrTableName = "";
      this.tempIncrColumnName = "";
      this.radio = "";
    },
    handlePreviewDdl: function () {
      if (!this.canPreviewDdl) return;
      var tablesToPreview = [];
      var includeOrExclude = this.dataform.includeOrExclude;
      if (includeOrExclude === 'INCLUDE') {
        // 包含模式：选了表则用选中的，否则用全部表
        tablesToPreview = this.dataform.sourceTables && this.dataform.sourceTables.length > 0
          ? this.dataform.sourceTables.slice()
          : JSON.parse(JSON.stringify(this.sourceSchemaTables));
      } else if (includeOrExclude === 'EXCLUDE') {
        // 排除模式：全部表去掉排除的
        tablesToPreview = JSON.parse(JSON.stringify(this.sourceSchemaTables));
        if (this.dataform.sourceTables && this.dataform.sourceTables.length > 0) {
          var excludeSet = this.dataform.sourceTables;
          tablesToPreview = tablesToPreview.filter(function (t) {
            return excludeSet.indexOf(t) < 0;
          });
        }
      } else {
        // 未选配置方式：使用全部表
        tablesToPreview = JSON.parse(JSON.stringify(this.sourceSchemaTables));
      }
      if (tablesToPreview.length === 0) {
        this.$message.warning('当前没有可预览的表，请检查源端模式下的表列表或调整表名配置');
        return;
      }
      // 前端已将 EXCLUDE 模式计算为最终净表列表，统一以 isInclude=true 传给后端
      var isIncludeForApi = true;
      var self = this;
      var tableNameMapper = this.dataform.tableNameMapper || [];
      var tableNameCase = this.dataform.tableNameCase || 'NONE';
      this.$http({
        method: "POST",
        headers: { 'Content-Type': 'application/json' },
        url: "/dbswitch/admin/api/v1/mapper/preview/table",
        data: JSON.stringify({
          id: this.dataform.sourceConnectionId,
          schemaName: this.dataform.sourceSchema,
          isInclude: isIncludeForApi,
          tableNames: tablesToPreview,
          nameMapper: tableNameMapper,
          tableNameCase: tableNameCase
        })
      }).then(function (res) {
        if (0 === res.data.code) {
          var tableInfoList = res.data.data.map(function (item) {
            return { sourceTableName: item.originalName, targetTableName: item.targetName };
          });
          self.ddlPreviewRequestParams = {
            sourceConnectionId: self.dataform.sourceConnectionId,
            sourceSchema: self.dataform.sourceSchema,
            targetConnectionId: self.dataform.targetConnectionId,
            targetSchema: self.dataform.targetSchema,
            tableNameMapper: tableNameMapper,
            columnNameMapper: self.dataform.columnNameMapper || [],
            tableNameCase: tableNameCase,
            columnNameCase: self.dataform.columnNameCase || 'NONE',
            targetAutoIncrement: self.dataform.targetAutoIncrement || false
          };
          self.ddlPreviewDialogVisible = true;
          self.$nextTick(function () {
            self.$refs['ddlPreviewDialogRef'].loadTableList(tableInfoList, self.dataform.customDdlMap);
          });
        } else {
          self.$message.error(res.data.message || '获取表名映射失败');
        }
      }).catch(function (error) {
        self.$message.error('获取表名映射失败: ' + (error.message || '网络错误'));
      });
    },
    handleDdlConfirm: function (customDdlMap, modifiedCount) {
      this.dataform.customDdlMap = customDdlMap;
      this.customDdlModifiedCount = modifiedCount;
    },
    handleSave: function () {
      if (0 === this.dataform.autoSyncMode) {
        this.dataform.targetDropTable = false;
        this.dataform.targetOnlyCreate = false;
      } else if (1 === this.dataform.autoSyncMode) {
        this.dataform.targetDropTable = true;
        this.dataform.targetOnlyCreate = true;
      } else {
        this.dataform.targetDropTable = true;
        this.dataform.targetOnlyCreate = false;
      }
      this.$refs['dataform'].validate(valid => {
        if (valid) {
          const configPayload = {
            sourceConnectionId: this.dataform.sourceConnectionId,
            sourceSchema: this.dataform.sourceSchema,
            tableType: this.dataform.tableType,
            includeOrExclude: this.dataform.includeOrExclude,
            sourceTables: this.dataform.sourceTables,
            incrTableColumns: this.dataform.incrTableColumns,
            sourceBeforeSqlScripts: this.dataform.sourceBeforeSqlScripts,
            sourceAfterSqlScripts: this.dataform.sourceAfterSqlScripts,
            targetConnectionId: this.dataform.targetConnectionId,
            targetSchema: this.dataform.targetSchema,
            tableNameMapper: this.dataform.tableNameMapper,
            columnNameMapper: this.dataform.columnNameMapper,
            tableNameCase: this.dataform.tableNameCase,
            columnNameCase: this.dataform.columnNameCase,
            targetDropTable: this.dataform.targetDropTable,
            targetOnlyCreate: this.dataform.targetOnlyCreate,
            targetAutoIncrement: this.dataform.targetAutoIncrement,
            batchSize: this.dataform.batchSize,
            channelSize: this.dataform.channelSize,
            targetSyncOption: this.dataform.targetSyncOption,
            targetBeforeSqlScripts: this.dataform.targetBeforeSqlScripts,
            targetAfterSqlScripts: this.dataform.targetAfterSqlScripts,
            customDdlMap: this.dataform.customDdlMap,
          };
          if (this.$route.query.id && this.$route.query.id > 0) {
            this.$http({
              method: "POST",
              headers: { 'Content-Type': 'application/json' },
              url: "/dbswitch/admin/api/v1/assignment/update",
              data: JSON.stringify({
                id: this.$route.query.id,
                name: this.dataform.name,
                description: this.dataform.description,
                scheduleMode: this.dataform.scheduleMode,
                cronExpression: this.dataform.cronExpression,
                config: configPayload
              })
            }).then(res => {
              if (0 === res.data.code) {
                this.$message({ message: '修改任务成功!', type: 'success' });
                this.$router.push('/task/list');
              } else {
                if (res.data.message) this.$message.error(res.data.message);
              }
            });
          } else {
            this.$http({
              method: "POST",
              headers: { 'Content-Type': 'application/json' },
              url: "/dbswitch/admin/api/v1/assignment/create",
              data: JSON.stringify({
                name: this.dataform.name,
                description: this.dataform.description,
                scheduleMode: this.dataform.scheduleMode,
                cronExpression: this.dataform.cronExpression,
                config: configPayload
              })
            }).then(res => {
              if (0 === res.data.code) {
                this.$message({ message: '添加任务成功!', type: 'success' });
                this.$router.push('/task/list');
              } else {
                if (res.data.message) this.$message.error(res.data.message);
              }
            });
          }
        } else {
          this.$message.warning("请点击【上一步】检查并填写必填项");
        }
      });
    }
  },
  created () {
    this.loadConnections();
    this.loadAssignmentDetail();
  },
}
</script>

<style scoped>
.edit-page {
  padding: 4px 0;
}

.edit-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 0 12px;
  margin-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.header-left {
  display: flex;
  align-items: center;
}

.back-btn {
  font-size: 14px;
  padding: 0;
}

.header-divider {
  margin: 0 12px;
  color: #dcdfe6;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.step-hint {
  font-size: 13px;
  color: #909399;
  background: #f5f7fa;
  padding: 4px 12px;
  border-radius: 12px;
}

.edit-card {
  width: 100%;
}

.steps-wrapper {
  padding: 16px 0 24px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
}

.edit-form {
  padding: 0;
}

.step-content {
  padding: 20px 0 0;
}

.step-section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 2px solid #409EFF;
  display: inline-block;
}

.step-section-title i {
  margin-right: 6px;
  color: #409EFF;
}

.form-area {
  max-width: 700px;
}

.field-tips {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.5;
}

.sync-mode-cards {
  display: flex;
  gap: 12px;
  padding-left: 0;
  flex-wrap: wrap;
}

.sync-mode-card {
  flex: 1;
  min-width: 200px;
  border: 2px solid #dcdfe6;
  border-radius: 8px;
  padding: 14px 16px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  position: relative;
  background: #fff;
}

.sync-mode-card:hover {
  border-color: #409EFF;
  box-shadow: 0 2px 8px rgba(64,158,255,0.15);
}

.sync-mode-card.is-selected {
  border-color: #409EFF;
  background: #ecf5ff;
}

.sync-mode-card .card-icon {
  font-size: 22px;
  color: #409EFF;
  padding-top: 2px;
}

.sync-mode-card .card-body {
  flex: 1;
}

.sync-mode-card .card-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.sync-mode-card .card-desc {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.sync-mode-card .card-check {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 18px;
  height: 18px;
  background: #409EFF;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 11px;
}

.incr-config-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.sql-collapse,
.advanced-collapse {
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

/deep/ .sql-collapse .el-collapse-item__header,
/deep/ .advanced-collapse .el-collapse-item__header {
  padding-left: 12px;
  background: #fafafa;
  color: #606266;
}

.advanced-title {
  font-size: 13px;
  color: #606266;
}

.mapper-tabs {
  margin-top: 0;
}

.tab-action-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 4px;
}

.table-empty-text {
  color: #909399;
  font-size: 13px;
}

/* 底部操作栏 */
.footer-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 32px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.footer-right {
  display: flex;
  gap: 10px;
}

/* 步骤条鼠标样式 */
/deep/ .el-step__head {
  cursor: pointer;
}

/deep/ .el-step__title {
  cursor: pointer;
}
</style>
