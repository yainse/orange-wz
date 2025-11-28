export function removeNodeById(data: [], id: number) {
  // 遍历根数组
  for (let i = 0; i < data.length; i++) {
    if (data[i].id === id) {
      // 如果找到匹配的节点，从数组中移除
      data.splice(i, 1);
      return true;
    }

    // 如果有子节点，递归查找
    if (data[i].children && data[i].children.length > 0) {
      if (removeNodeById(data[i].children, id)) {
        return true;
      }
    }
  }
  return false;
}

export function renameNodeById(data: [], id: number, name: string) {
  // 遍历根数组
  for (let i = 0; i < data.length; i++) {
    if (data[i].id === id) {
      data[i].name = name;
      return true;
    }

    // 如果有子节点，递归查找
    if (data[i].children && data[i].children.length > 0) {
      if (renameNodeById(data[i].children, id, name)) {
        return true;
      }
    }
  }
  return false;
}

export function getNodeById(data: [], id: number) {
  // 遍历根数组
  for (let i = 0; i < data.length; i++) {
    if (data[i].id === id) {
      return data[i];
    }

    // 如果有子节点，递归查找
    if (data[i].children && data[i].children.length > 0) {
      const result = getNodeById(data[i].children, id);
      if (result) return result;
    }
  }
  return null;
}

export function getBrotherByTwoId(data: [], id1: number, id2: number) {
  // 遍历根数组
  let count = 0;
  for (let i = 0; i < data.length; i++) {
    if (data[i].id == id1) count++;
    else if (data[i].id == id2) count++;

    if (count == 2) return data;

    // 如果有子节点，递归查找
    if (count == 0 && data[i].children && data[i].children.length > 0) {
      const result = getBrotherByTwoId(data[i].children, id1, id2);
      if (result === undefined) return undefined;
      else if (result) return result;
    }
  }

  if (count == 1) return undefined; // 不在同一个
  return null;
}
