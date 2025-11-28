<template>
  <el-dialog v-model="visible" title="打开" width="500">
    <p>每页最多显示100个文件/文件夹，文件多时建议打开整个文件夹</p>
    <p>文件变动没有更新时，点击刷新缓存</p>
    <el-button type="success" @click="reloadClick" style="margin: 10px 0">刷新缓存</el-button>
    <el-breadcrumb separator="/" style="margin: 10px 0">
      <el-breadcrumb-item v-for="item in nav" :key="item.id">
        <a @click="navClick(item.id)">{{ item.name }}</a>
      </el-breadcrumb-item>
    </el-breadcrumb>
    <el-table :data="tableData" height="50vh">
      <el-table-column label="文件">
        <template #default="scope">
          <div class="file-item">
            <FcFolder width="20" height="20" v-if="scope.row.type == 'FOLDER'" class="file-icon" />
            <BsFileEarmarkWord width="20" height="20" v-else class="file-icon" />
            {{ scope.row.name }}
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" :width="160" align="center">
        <template #default="scope">
          <el-button
            v-show="scope.row.type == 'FOLDER'"
            type="success"
            @click="watchClick(scope.row)"
            size="small"
          >
            查看
          </el-button>
          <el-button type="primary" @click="openClick(scope.row)" size="small">载入</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>
  <WzKey ref="WzKeyRef" @confirmWzKey="confirmWzKey" />
</template>

<script setup lang="ts">
  import { type GetWzDto, open, reloadFolder, watchFolder } from '@/api/wz.ts';
  import WzKey from '@/views/wz/WzKey.vue';
  import { ref, useTemplateRef } from 'vue';
  import { BsFileEarmarkWord } from 'vue-icons-plus/bs';
  import { FcFolder } from 'vue-icons-plus/fc';

  const pFunc = defineEmits(['navClick']);

  const nav = ref<GetWzDto[]>([{ name: 'root', id: -1, type: 'FOLDER', children: [], leaf: true }]);
  const visible = ref(false);
  const tableData = ref<GetWzDto[]>([]);

  const openReady = ref<GetWzDto | undefined>();
  const wzKeyRef = useTemplateRef('WzKeyRef');

  const initial = async () => {
    nav.value = [{ name: 'root', id: -1, type: 'FOLDER', children: [], leaf: true }];
    await loadTableData(-1);
    visible.value = true;
  };
  defineExpose({ initial });

  const loadTableData = async (id: number) => {
    const { data } = await watchFolder(id);
    tableData.value = data.data;
  };

  const navClick = (id: number) => {
    loadTableData(id);
    removeNav(nav.value, id);
  };

  const reloadClick = async () => {
    await reloadFolder();
    nav.value = [{ name: 'root', id: -1, type: 'FOLDER', children: [], leaf: true }];
    await loadTableData(-1);
    ElMessage.success({ message: '数据已更新' });
  };

  const watchClick = (row: GetWzDto) => {
    loadTableData(row.id);
    nav.value.push(row);
  };

  const openClick = async (row: GetWzDto) => {
    openReady.value = row;
    wzKeyRef.value.initial();
  };

  const confirmWzKey = async (version: number, key: string) => {
    if (openReady.value) {
      await open(openReady.value.id, version, key);
      pFunc('navClick', 0);
      visible.value = false;
    }
  };

  const removeNav = (arr: GetWzDto[], targetValue: number) => {
    const index = arr.findIndex((item) => item.id === targetValue);
    if (index !== -1) arr.splice(index + 1); // 从 index+1 开始删除后面的所有元素

    return arr;
  };
</script>

<style scoped></style>
