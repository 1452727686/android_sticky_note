# AI 便利贴 API 文档

本文档基于源码自动生成，涵盖设备管理、待办事项和显示推送等模块的全部接口功能、请求方式、路径、参数及响应格式。

## 目录

- [AI 便利贴 API 文档](#ai-便利贴-api-文档)
  - [目录](#目录)
  - [全局说明](#全局说明)
  - [设备管理](#设备管理)
    - [获取设备列表](#获取设备列表)
  - [待办事项](#待办事项)
    - [获取待办列表](#获取待办列表)
    - [创建待办](#创建待办)
    - [更新待办](#更新待办)
    - [标记完成/取消完成](#标记完成取消完成)
    - [删除待办](#删除待办)
  - [显示推送](#显示推送)
    - [推送图片到设备](#推送图片到设备)
    - [推送文本到设备](#推送文本到设备)
    - [推送标题+正文到设备](#推送标题正文到设备)
    - [删除页面](#删除页面)

## 全局说明

- **接口返回码 (Code)**: 响应体中 `"code": 0` 表示请求成功。错误处理和其它业务状态码需结合实际业务判断。
- **请求与响应类型**: 除特殊说明（如 `multipart`）外，通常采用 `application/json` 格式进行数据交互。

---

## 设备管理

### 获取设备列表

- **接口描述**: 获取设备列表
- **请求方式**: `GET`
- **请求路径**: `/open/v1/devices`

**响应示例**

```json
{
  "code": 0,
  "data": [
    {
      "deviceId": "AA:BB:CC:DD:EE:FF",
      "alias": "我的设备",
      "board": "bread-compact-wifi"
    }
  ]
}
```

---

## 待办事项

### 获取待办列表

- **接口描述**: 获取待办列表
- **请求方式**: `GET`
- **请求路径**: `/open/v1/todos`
- **调用场景**: 页面初始加载 / 定时自动同步（按 `syncInterval` 周期轮询） / 用户手动刷新

**Query 参数**

| 参数名 | 类型 | 必填 | 说明 | 示例 |
| :--- | :--- | :---: | :--- | :--- |
| `status` | integer | 否 | 过滤状态：0=待完成, 1=已完成 | `0` |
| `deviceId` | string | 否 | 设备ID(MAC地址)，过滤指定设备的待办 | `AA:BB:CC:DD:EE:FF` |

**响应示例**

```json
{
  "code": 0,
  "data": [
    {
      "id": 1,
      "title": "买牛奶",
      "description": "",
      "dueDate": "2026-03-20",
      "dueTime": "09:00",
      "repeatType": "none",
      "status": 0,
      "priority": 1,
      "completed": false,
      "deviceId": "AA:BB:CC:DD:EE:FF",
      "deviceName": "我的设备",
      "createDate": "2026-03-18 10:00:00",
      "updateDate": 1742284800
    }
  ]
}
```

### 创建待办

- **接口描述**: 创建待办
- **请求方式**: `POST`
- **请求路径**: `/open/v1/todos`

**Body 参数**

| 参数名 | 类型 | 必填 | 说明 | 示例 |
| :--- | :--- | :---: | :--- | :--- |
| `title` | string | 是 | 标题 | `买牛奶` |
| `description` | string | 否 | 描述 | `""` |
| `dueDate` | string | 否 | 截止日期(yyyy-MM-dd) | `2026-03-20` |
| `dueTime` | string | 否 | 截止时间(HH:mm) | `09:00` |
| `repeatType` | string | 否 | 重复类型：daily/weekly/monthly/yearly/none | `none` |
| `repeatWeekday` | integer | 否 | 周几 0-6, 0=周日（weekly用） | |
| `repeatMonth` | integer | 否 | 每年几月 1-12（yearly用） | |
| `repeatDay` | integer | 否 | 每月几号 1-31（monthly/yearly用） | |
| `priority` | integer | 否 | 优先级：0=普通, 1=重要, 2=紧急 | `1` |
| `deviceId` | string | 否 | 设备ID(MAC地址)，为空则为个人待办 | `AA:BB:CC:DD:EE:FF` |

**响应示例**

```json
{
  "code": 0,
  "data": {
    "id": 1,
    "title": "买牛奶",
    "status": 0,
    "priority": 1,
    "deviceId": "AA:BB:CC:DD:EE:FF",
    "createDate": "2026-03-18 10:00:00"
  }
}
```

### 更新待办

- **接口描述**: 更新待办
- **请求方式**: `PUT`
- **请求路径**: `/open/v1/todos/{id}`

**Path 参数**

| 参数名 | 类型 | 必填 | 说明 | 示例 |
| :--- | :--- | :---: | :--- | :--- |
| `id` | integer | 是 | 待办ID | `1` |

**Body 参数**

| 参数名 | 类型 | 必填 | 说明 | 示例 |
| :--- | :--- | :---: | :--- | :--- |
| `title` | string | 否 | 标题 | `买牛奶和面包` |
| `description` | string | 否 | 描述 | |
| `dueDate` | string | 否 | 截止日期(yyyy-MM-dd) | |
| `dueTime` | string | 否 | 截止时间(HH:mm) | |
| `priority` | integer | 否 | 优先级：0=普通, 1=重要, 2=紧急 | |

**响应示例**

```json
{
  "code": 0,
  "data": {
    "id": 1,
    "title": "买牛奶和面包",
    "status": 0,
    "priority": 1
  }
}
```

### 标记完成/取消完成

- **接口描述**: 标记完成/取消完成
- **请求方式**: `PUT`
- **请求路径**: `/open/v1/todos/{id}/complete`

**Path 参数**

| 参数名 | 类型 | 必填 | 说明 | 示例 |
| :--- | :--- | :---: | :--- | :--- |
| `id` | integer | 是 | 待办ID | `1` |

**响应示例**

```json
{
  "code": 0,
  "msg": "success"
}
```

### 删除待办

- **接口描述**: 删除待办
- **请求方式**: `DELETE`
- **请求路径**: `/open/v1/todos/{id}`

**Path 参数**

| 参数名 | 类型 | 必填 | 说明 | 示例 |
| :--- | :--- | :---: | :--- | :--- |
| `id` | integer | 是 | 待办ID | `1` |

**响应示例**

```json
{
  "code": 0,
  "msg": "success"
}
```

---

## 显示推送

### 推送图片到设备

- **接口描述**: 推送图片到设备
- **请求方式**: `POST`
- **请求路径**: `/open/v1/devices/{deviceId}/display/image`
- **请求类型**: `multipart/form-data`

**Path 参数**

| 参数名 | 类型 | 必填 | 说明 | 示例 |
| :--- | :--- | :---: | :--- | :--- |
| `deviceId` | string | 是 | 设备ID(MAC地址) | `AA:BB:CC:DD:EE:FF` |

**Body 参数 (multipart)**

| 参数名 | 类型 | 必填 | 说明 | 示例 |
| :--- | :--- | :---: | :--- | :--- |
| `images` | file | 是 | 图片文件，支持多张(最多5张)，单张不超过2MB | |
| `dither` | boolean | 否 | 是否使用抖动算法(默认true)，关闭则使用硬阈值二值化 | `true` |
| `pageId` | string | 否 | 页面编号(1-5)，指定后会持久化存储 | `1` |

**响应示例**

```json
{
  "code": 0,
  "data": {
    "totalPages": 1,
    "pushedPages": 1,
    "pageId": "1"
  }
}
```

### 推送文本到设备

- **接口描述**: 推送文本到设备
- **请求方式**: `POST`
- **请求路径**: `/open/v1/devices/{deviceId}/display/text`

**Path 参数**

| 参数名 | 类型 | 必填 | 说明 | 示例 |
| :--- | :--- | :---: | :--- | :--- |
| `deviceId` | string | 是 | 设备ID(MAC地址) | `AA:BB:CC:DD:EE:FF` |

**Body 参数**

| 参数名 | 类型 | 必填 | 说明 | 示例 |
| :--- | :--- | :---: | :--- | :--- |
| `text` | string | 是 | 文本内容(最多5000字)，支持换行 | `今日天气：晴\n温度：25°C` |
| `fontSize` | integer | 否 | 字体大小(12-48，默认20) | `20` |
| `pageId` | string | 否 | 页面编号(1-5)，指定后会持久化存储 | `1` |

**响应示例**

```json
{
  "code": 0,
  "data": {
    "totalPages": 1,
    "pushedPages": 1,
    "pageId": "1"
  }
}
```

### 推送标题+正文到设备

- **接口描述**: 推送标题+正文到设备
- **请求方式**: `POST`
- **请求路径**: `/open/v1/devices/{deviceId}/display/structured-text`

**Path 参数**

| 参数名 | 类型 | 必填 | 说明 | 示例 |
| :--- | :--- | :---: | :--- | :--- |
| `deviceId` | string | 是 | 设备ID(MAC地址) | `AA:BB:CC:DD:EE:FF` |

**Body 参数**

| 参数名 | 类型 | 必填 | 说明 | 示例 |
| :--- | :--- | :---: | :--- | :--- |
| `title` | string | 否 | 标题文本(最多200字)，与body至少填一项 | `会议提醒` |
| `body` | string | 否 | 正文内容(最多5000字)，支持换行 | `15:00 三楼会议室\n请带笔记本` |
| `pageId` | string | 否 | 页面编号(1-5)，指定后会持久化存储 | `1` |

**响应示例**

```json
{
  "code": 0,
  "data": {
    "totalPages": 1,
    "pushedPages": 1,
    "pageId": "1"
  }
}
```

### 删除页面

- **接口描述**: 删除页面(不传pageId则清空全部)
- **请求方式**: `DELETE`
- **请求路径**: `/open/v1/devices/{deviceId}/display/pages/{pageId}`

**Path 参数**

| 参数名 | 类型 | 必填 | 说明 | 示例 |
| :--- | :--- | :---: | :--- | :--- |
| `deviceId` | string | 是 | 设备ID(MAC地址) | `AA:BB:CC:DD:EE:FF` |
| `pageId` | string | 否 | 页面编号，不传则删除全部页面 | `1` |

**响应示例**

```json
{
  "code": 0,
  "msg": "success"
}
```
