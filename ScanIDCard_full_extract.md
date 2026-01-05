# ScanIDCard.pptx 全量内容提取

源文件：ScanIDCard.pptx

## 第 1 页

- ID Card Identification

## 第 2 页

- 身份证识别总体流程
- 利用摄像头拍照
- 获取腾讯云服务器返回数据
- 图片转换为Base64格式
- 利用腾讯云V3签名加密图片
- 利用handler解析json数据

## 第 3 页

- 详细代码流程描述
- 创建button，设计点击事件
- 启用相机捕获图片
- 获取身份证详细信息
- handler处理信息
- 设计点击事件
- 保存拍摄图片
- 将图片进行处理
- V3签名函数封装
- 将图片转换成Base64位数据
- 对图片进行加密封装
- 向腾讯云发起post请求
- 将请求得到哦的数据进行解析

### 本页包含的图片（已导出）

- slide03_210218dea3.png
- slide03_5a38896578.png
- slide03_70d181e9c8.png
- slide03_737914cd8d.png
- slide03_811f3a7f3e.png
- slide03_8eed0b38ac.png
- slide03_931ba76451.png
- slide03_965f863ed4.png

## 第 4 页

- V3签名详解
  为什么要进行签名
  签名通过以下方式帮助保护请求：

1.验证请求者的身份
签名确保请求是由持有有效访问密钥的人发送的。请参阅控制台 云 API 密钥 页面获取密钥相关信息。

2.保护传输中的数据
为了防止请求在传输过程中被篡改，腾讯云 API 会使用请求参数来计算请求的哈希值，并将生成的哈希值加密后作为请求的一部分，发送到腾讯云 API 服务器。服务器会使用收到的请求参数以同样的过程计算哈希值，并验证请求中的哈希值。如果请求被篡改，将导致哈希值不一致，腾讯云 API 将拒绝本次请求。

详细了解请参考：https://cloud.tencent.com/document/product/213/30654

## 第 5 页

- V3签名详解
  申请安全凭证

本文使用的安全凭证为密钥，密钥包括 SecretId 和 SecretKey。每个用户最多可以拥有两对密钥。

SecretId：用于标识 API 调用者身份，可以简单类比为用户名。
SecretKey：用于验证 API 调用者的身份，可以简单类比为密码。
用户必须严格保管安全凭证，避免泄露，否则将危及财产安全。如已泄漏，请立刻禁用该安全凭证。

## 第 6 页

- V3签名详解
  签名版本 v3 签名过程
  云 API 支持 GET 和 POST 请求。对于GET方法，只支持 Content-Type: application/x-www-form-urlencoded 协议格式。对于POST方法，目前支持 Content-Type: application/json 以及 Content-Type: multipart/form-data 两种协议格式，json 格式绝大多数接口均支持，multipart 格式只有特定接口支持，此时该接口不能使用 json 格式调用，参考具体业务接口文档说明。推荐使用 POST 请求，因为两者的结果并无差异，但 GET 请求只支持 32 KB 以内的请求包。

下面以云服务器查询广州区实例列表作为例子，分步骤介绍签名的计算过程。我们选择该接口是因为：

云服务器默认已开通，该接口很常用；
该接口是只读的，不会改变现有资源的状态；
接口覆盖的参数种类较全，可以演示包含数据结构的数组如何使用。

## 第 7 页

- V3签名详解

```java
// ************* 步骤 1：拼接规范请求串 *************
    private static String HTTPRequestMethod = "POST";
    private static String CanonicalURI = "/";
    private static String CanonicalQueryString = "";
    private static String CanonicalHeaders = "content-type:application/json;    charset=utf-8\nhost:ocr.tencentcloudapi.com\n";
    private static String SignedHeaders = "content-type;host";//参与签名的头部信息
```

## 第 8 页

- V3签名详解

```java
// ************* 步骤 2：拼接待签名字符串 *************
            String credentialScope = dateString + "/" + Service + "/" + Stop;
            String hashedCanonicalRequest = HashEncryption(CanonicalRequest);
            String stringToSign = Algorithm + "\n" +
                    timestamp + "\n" +
                    credentialScope + "\n" +
                    hashedCanonicalRequest;
```

## 第 9 页

- V3签名详解

```java
// ************* 步骤 3：计算签名 *************
            byte[] secretDate = HashHmacSha256Encryption(("TC3" + SecretKey).getBytes("UTF-8"), dateString);
            byte[] secretService = HashHmacSha256Encryption(secretDate, Service);
            byte[] secretSigning = HashHmacSha256Encryption(secretService, Stop);
```

## 第 10 页

- V3签名详解

```java
// ************* 步骤 4：拼接 Authorization *************
            String authorization = Algorithm + ' ' +
                    "Credential=" + SecretId + '/' + credentialScope + ", " +
                    "SignedHeaders=" + SignedHeaders + ", " +
                    "Signature=" + signature;
```


# 以下为腾讯云身份证识别文档

最近更新时间：2025-12-25 01:44:10

[](https://main.qcloudimg.com/raw/document/product/pdf/866_17603_cn.pdf)[]()*  ![]()

[]()[*我的收藏*]()

## 本页目录：

* [1. 接口描述](https://cloud.tencent.com/document/product/866/33524#1.-.E6.8E.A5.E5.8F.A3.E6.8F.8F.E8.BF.B0 "1. 接口描述")
* [2. 输入参数](https://cloud.tencent.com/document/product/866/33524#2.-.E8.BE.93.E5.85.A5.E5.8F.82.E6.95.B0 "2. 输入参数")
* [3. 输出参数](https://cloud.tencent.com/document/product/866/33524#3.-.E8.BE.93.E5.87.BA.E5.8F.82.E6.95.B0 "3. 输出参数")
* [4. 示例](https://cloud.tencent.com/document/product/866/33524#4.-.E7.A4.BA.E4.BE.8B "4. 示例")
  * [示例1 临时身份证告警调用示例](https://cloud.tencent.com/document/product/866/33524#.E7.A4.BA.E4.BE.8B1-.E4.B8.B4.E6.97.B6.E8.BA.AB.E4.BB.BD.E8.AF.81.E5.91.8A.E8.AD.A6.E8.B0.83.E7.94.A8.E7.A4.BA.E4.BE.8B "示例1 临时身份证告警调用示例")
  * [示例2 身份证识别（人像面）调用示例](https://cloud.tencent.com/document/product/866/33524#.E7.A4.BA.E4.BE.8B2-.E8.BA.AB.E4.BB.BD.E8.AF.81.E8.AF.86.E5.88.AB.EF.BC.88.E4.BA.BA.E5.83.8F.E9.9D.A2.EF.BC.89.E8.B0.83.E7.94.A8.E7.A4.BA.E4.BE.8B "示例2 身份证识别（人像面）调用示例")
  * [示例3 身份证识别（国徽面）调用示例](https://cloud.tencent.com/document/product/866/33524#.E7.A4.BA.E4.BE.8B3-.E8.BA.AB.E4.BB.BD.E8.AF.81.E8.AF.86.E5.88.AB.EF.BC.88.E5.9B.BD.E5.BE.BD.E9.9D.A2.EF.BC.89.E8.B0.83.E7.94.A8.E7.A4.BA.E4.BE.8B "示例3 身份证识别（国徽面）调用示例")
  * [示例4 身份证照片裁剪和人像照片裁剪调用示例](https://cloud.tencent.com/document/product/866/33524#.E7.A4.BA.E4.BE.8B4-.E8.BA.AB.E4.BB.BD.E8.AF.81.E7.85.A7.E7.89.87.E8.A3.81.E5.89.AA.E5.92.8C.E4.BA.BA.E5.83.8F.E7.85.A7.E7.89.87.E8.A3.81.E5.89.AA.E8.B0.83.E7.94.A8.E7.A4.BA.E4.BE.8B "示例4 身份证照片裁剪和人像照片裁剪调用示例")
* [5. 开发者资源](https://cloud.tencent.com/document/product/866/33524#5.-.E5.BC.80.E5.8F.91.E8.80.85.E8.B5.84.E6.BA.90 "5. 开发者资源")
  * [腾讯云 API 平台](https://cloud.tencent.com/document/product/866/33524#.E8.85.BE.E8.AE.AF.E4.BA.91-API-.E5.B9.B3.E5.8F.B0 "腾讯云 API 平台")
  * [API Inspector](https://cloud.tencent.com/document/product/866/33524#API-Inspector "API Inspector")
  * [SDK](https://cloud.tencent.com/document/product/866/33524#SDK "SDK")
  * [命令行工具](https://cloud.tencent.com/document/product/866/33524#.E5.91.BD.E4.BB.A4.E8.A1.8C.E5.B7.A5.E5.85.B7 "命令行工具")
* [6. 错误码](https://cloud.tencent.com/document/product/866/33524#6.-.E9.94.99.E8.AF.AF.E7.A0.81 "6. 错误码")

## 1. 接口描述

接口请求域名： ocr.tencentcloudapi.com 。

本接口支持中国大陆居民二代身份证正反面所有字段的识别，包括姓名、性别、民族、出生日期、住址、公民身份证号、签发机关、有效期限，识别准确度达到99%以上。

另外，本接口还支持多种扩展能力，满足不同场景的需求。如身份证照片、人像照片的裁剪功能，同时具备7种告警功能，如下表所示。


| 扩展能力                               | 能力项                                                   |
| -------------------------------------- | -------------------------------------------------------- |
| 裁剪功能                               | 身份证照片裁剪（去掉证件外多余的边缘、自动矫正拍摄角度） |
| 人像照片裁剪（自动抠取身份证头像区域） |                                                          |
| 告警功能                               |                                                          |
| 身份证边框不完整告警                   |                                                          |
| 身份证复印件告警                       |                                                          |
| 身份证翻拍告警                         |                                                          |
| 身份证框内遮挡告警                     |                                                          |
| 临时身份证告警                         |                                                          |
| 身份证疑似存在PS痕迹告警               |                                                          |
| 图片模糊告警（可根据图片质量分数判断） |                                                          |

默认接口请求频率限制：20次/秒。

推荐使用 API Explorer

[点击调试](https://console.cloud.tencent.com/api/explorer?Product=ocr&Version=2018-11-19&Action=IDCardOCR)

API Explorer 提供了在线调用、签名验证、SDK 代码生成和快速检索接口等能力。您可查看每次调用的请求内容和返回结果以及自动生成 SDK 调用示例。

## 2. 输入参数

以下请求参数列表仅列出了接口请求参数和部分公共参数，完整公共参数列表见 [公共请求参数](https://cloud.tencent.com/document/api/866/33518)。


| 参数名称                 | 必选 | 类型    | 描述                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| ------------------------ | ---- | ------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Action                   | 是   | String  | [公共参数](https://cloud.tencent.com/document/api/866/33518)，本接口取值：IDCardOCR。                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| Version                  | 是   | String  | [公共参数](https://cloud.tencent.com/document/api/866/33518)，本接口取值：2018-11-19。                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| Region                   | 否   | String  | [公共参数](https://cloud.tencent.com/document/api/866/33518)，此参数为可选参数。                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| ImageBase64              | 否   | String  | 图片的 Base64 值。要求图片经Base64编码后不超过 10M，分辨率建议500\*800以上，支持PNG、JPG、JPEG、BMP格式。建议卡片部分占据图片2/3以上。图片的 ImageUrl、ImageBase64 必须提供一个，如果都提供，只使用 ImageUrl。<br/>示例值：/9j/4AAQSkZJRg.....s97n//2Q==                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| ImageUrl                 | 否   | String  | 图片的 Url 地址。要求图片经Base64编码后不超过 10M，分辨率建议500\*800以上，支持PNG、JPG、JPEG、BMP格式。建议卡片部分占据图片2/3以上。建议图片存储于腾讯云，可保障更高的下载速度和稳定性。<br/>示例值：https://ocr-demo-1254418846.cos.ap-guangzhou.myqcloud.com/card/IDCardOCR/IDCardOCR1.jpg                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| CardSide                 | 否   | String  | FRONT：身份证有照片的一面（人像面），<br/>BACK：身份证有国徽的一面（国徽面），<br/>该参数如果不填，将为您自动判断身份证正反面。<br/>示例值：FRONT                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| Config                   | 否   | String  | 以下可选字段均为bool 类型，默认false：<br/>CropIdCard，身份证照片裁剪（去掉证件外多余的边缘、自动矫正拍摄角度）<br/>CropPortrait，人像照片裁剪（自动抠取身份证头像区域）<br/>CopyWarn，复印件告警<br/>BorderCheckWarn，边框和框内遮挡告警<br/>ReshootWarn，翻拍告警<br/>DetectPsWarn，疑似存在PS痕迹告警<br/>TempIdWarn，临时身份证告警<br/>InvalidDateWarn，身份证有效日期不合法告警<br/>Quality，图片质量分数（评价图片的模糊程度）<br/>MultiCardDetect，是否开启正反面同框识别（仅支持二代身份证正反页同框识别或临时身份证正反页同框识别）<br/>ReflectWarn，是否开启反光检测<br/><br/>SDK 设置方式参考：<br/>Config = Json.stringify({"CropIdCard":true,"CropPortrait":true})<br/>API 3.0 Explorer 设置方式参考：<br/>Config = {"CropIdCard":true,"CropPortrait":true}<br/>示例值：{"CropIdCard":true,"CropPortrait":true} |
| EnableRecognitionRectify | 否   | Boolean | 默认值为true，打开识别结果纠正开关。开关开启后，身份证号、出生日期、性别，三个字段会进行矫正补齐，统一结果输出；若关闭此开关，以上三个字段不会进行矫正补齐，保持原始识别结果输出，若原图出现篡改情况，这三个字段的识别结果可能会不统一。<br/>示例值：false                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| EnableReflectDetail      | 否   | Boolean | 默认值为false。<br/><br/>此开关需要在反光检测开关开启下才会生效（即此开关生效的前提是config入参里的"ReflectWarn":true），若EnableReflectDetail设置为true，则会返回反光点覆盖区域详情。反光点覆盖区域详情分为四部分：人像照片位置、国徽位置、识别字段位置、其他位置。一个反光点允许覆盖多个区域，且一张图片可能存在多个反光点。<br/>示例值：false                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| CardWarnType             | 否   | String  | Basic：使用基础卡证告警能力； Advanced：开启通用卡证鉴伪能力（需要在控制台开启“通用卡证鉴伪”后计费功能或购买“通用卡证鉴伪”资源包后才能使用），默认值为 Basic<br/>示例值：Basic                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |

## 3. 输出参数


| 参数名称           | 类型                                                                                            | 描述                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| ------------------ | ----------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Name               | String                                                                                          | 姓名（人像面）<br/>示例值：李明                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| Sex                | String                                                                                          | 性别（人像面）<br/>示例值：男                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| Nation             | String                                                                                          | 民族（人像面）<br/>示例值：汉                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| Birth              | String                                                                                          | 出生日期（人像面）<br/>示例值：1987/1/1                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| Address            | String                                                                                          | 地址（人像面）<br/>示例值：北京市石景山区高新技术园腾讯大楼                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| IdNum              | String                                                                                          | 身份证号（人像面）<br/>示例值：440524198701010014                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| Authority          | String                                                                                          | 发证机关（国徽面）<br/>示例值：深圳市公安局南山分局                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| ValidDate          | String                                                                                          | 证件有效期（国徽面）<br/>示例值：2017.08.12-2037.08.12                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| AdvancedInfo       | String                                                                                          | 扩展信息，不请求则不返回，具体输入参考示例3和示例4。<br/>IdCard，裁剪后身份证照片的base64编码，请求 Config.CropIdCard 时返回；<br/>Portrait，身份证头像照片的base64编码，请求 Config.CropPortrait 时返回；<br/><br/>Quality，图片质量分数，请求 Config.Quality 时返回（取值范围：0 \~ 100，分数越低越模糊，建议阈值≥50）;<br/>BorderCodeValue，身份证边框不完整告警阈值分数，请求 Config.BorderCheckWarn时返回（取值范围：0 \~ 100，分数越低边框遮挡可能性越低，建议阈值≤50）;<br/><br/>WarnInfos，告警信息，Code 告警码列表和释义：<br/>-9101 身份证边框不完整告警，<br/><br/>-9102 身份证复印件告警（黑白及彩色复印件）,<br/>-9108 身份证复印件告警（仅黑白复印件），<br/><br/>-9103 身份证翻拍告警，<br/>-9105 身份证框内遮挡告警，<br/>-9104 临时身份证告警，<br/>-9106 身份证疑似存在PS痕迹告警，<br/>-9107 身份证反光告警。<br/>示例值：{"Quality":100,"WarnInfos":[],"BorderCodeValue":26} |
| ReflectDetailInfos | Array of[ReflectDetailInfo](https://cloud.tencent.com/document/api/866/33527#ReflectDetailInfo) | 反光点覆盖区域详情结果，具体内容请点击左侧链接                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| RequestId          | String                                                                                          | 唯一请求 ID，由服务端生成，每次请求都会返回（若请求因其他原因未能抵达服务端，则该次请求不会获得 RequestId）。定位问题时需要提供该次请求的 RequestId。                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |

## 4. 示例

### 示例1 临时身份证告警调用示例

临时身份证告警调用示例 [前往调试工具](https://console.cloud.tencent.com/api/explorer?Product=ocr&Version=2018-11-19&Action=IDCardOCR)

#### 输入示例

```
POST / HTTP/1.1
Host: ocr.tencentcloudapi.com
Content-Type: application/json
X-TC-Action: IDCardOCR
<公共请求参数>

{
    "ImageUrl": "https://ocr-demo-1254418846.cos.ap-guangzhou.myqcloud.com/card/IDCardOCR/IDCardOCR1.jpg",
    "Config": "{\"TempIdWarn\":true}",
    "CardSide": "FRONT"
}
```

#### 输出示例

```json
{
    "Response": {
        "Address": "广东省深圳市南山区腾讯大厦",
        "AdvancedInfo": "{\"WarnInfos\":[]}",
        "Authority": "",
        "Birth": "1995/5/13",
        "IdNum": "440305199505132561",
        "Name": "刘洋",
        "Nation": "汉",
        "ReflectDetailInfos": [],
        "RequestId": "c762a670-c622-408a-865a-da27a9ffa53b",
        "Sex": "女",
        "ValidDate": ""
    }
}
```

### 示例2 身份证识别（人像面）调用示例

身份证识别（人像面）调用示例 [前往调试工具](https://console.cloud.tencent.com/api/explorer?Product=ocr&Version=2018-11-19&Action=IDCardOCR)

#### 输入示例

```
POST / HTTP/1.1
Host: ocr.tencentcloudapi.com
Content-Type: application/json
X-TC-Action: IDCardOCR
<公共请求参数>

{
    "ImageUrl": "https://ocr-demo-1254418846.cos.ap-guangzhou.myqcloud.com/card/IDCardOCR/IDCardOCR1.jpg",
    "CardSide": "FRONT"
}
```

#### 输出示例

```json
{
    "Response": {
        "Address": "广东省深圳市南山区腾讯大厦",
        "AdvancedInfo": "{\"WarnInfos\":[]}",
        "Authority": "",
        "Birth": "1995/5/13",
        "IdNum": "440305199505132561",
        "Name": "刘洋",
        "Nation": "汉",
        "ReflectDetailInfos": [],
        "RequestId": "c762a670-c622-408a-865a-da27a9ffa53b",
        "Sex": "女",
        "ValidDate": ""
    }
}
```

### 示例3 身份证识别（国徽面）调用示例

身份证识别（国徽面）调用示例 [前往调试工具](https://console.cloud.tencent.com/api/explorer?Product=ocr&Version=2018-11-19&Action=IDCardOCR)

#### 输入示例

```
POST / HTTP/1.1
Host: ocr.tencentcloudapi.com
Content-Type: application/json
X-TC-Action: IDCardOCR
<公共请求参数>

{
    "ImageUrl": "https://ocr-demo-1254418846.cos.ap-guangzhou.myqcloud.com/card/IDCardBackOCR/IDCardBackOCR2.jpg",
    "CardSide": "BACK"
}
```

#### 输出示例

```json
{
    "Response": {
        "Address": "",
        "AdvancedInfo": "{\"WarnInfos\":[]}",
        "Authority": "上海市公安局南山分局",
        "Birth": "",
        "IdNum": "",
        "Name": "",
        "Nation": "",
        "ReflectDetailInfos": [],
        "RequestId": "c058efd9-a469-4256-a18d-bf539fd2231b",
        "Sex": "",
        "ValidDate": "2018.08.12-2038.08.12"
    }
}
```

### 示例4 身份证照片裁剪和人像照片裁剪调用示例

身份证照片裁剪和人像照片裁剪调用示例 [前往调试工具](https://console.cloud.tencent.com/api/explorer?Product=ocr&Version=2018-11-19&Action=IDCardOCR)

#### 输入示例

```
POST / HTTP/1.1
Host: ocr.tencentcloudapi.com
Content-Type: application/json
X-TC-Action: IDCardOCR
<公共请求参数>

{
    "ImageUrl": "https://ocr-demo-1254418846.cos.ap-guangzhou.myqcloud.com/card/IDCardOCR/IDCardOCR1.jpg",
    "Config": "{\"CropIdCard\":true,\"CropPortrait\":true}",
    "CardSide": "FRONT"
}
```

#### 输出示例

```json
{
    "Response": {
        "Name": "李明",
        "Sex": "男",
        "Nation": "汉",
        "Birth": "1987/1/1",
        "Address": "北京市石景山区高新技术园腾讯大楼",
        "IdNum": "440524198701010014",
        "Authority": "",
        "ValidDate": "",
        "ReflectDetailInfos": [],
        "AdvancedInfo": "{\"IdCard\":\"/9j/4AAQSkZJRg.....s97n//2Q==\",\"Portrait\":\"/9j/4AAQSkZJRg.....s97n//2Q==\"}",
        "RequestId": "97c323da-5fd3-4fe6-b0b3-1cf10b04422c"
    }
}
```

## 5. 开发者资源

### 腾讯云 API 平台

[腾讯云 API 平台](https://cloud.tencent.com/api) 是综合 API 文档、错误码、API Explorer 及 SDK 等资源的统一查询平台，方便您从同一入口查询及使用腾讯云提供的所有 API 服务。

### API Inspector

用户可通过 [API Inspector](https://cloud.tencent.com/document/product/1278/49361) 查看控制台每一步操作关联的 API 调用情况，并自动生成各语言版本的 API 代码，也可前往 [API Explorer](https://cloud.tencent.com/document/product/1278/46697) 进行在线调试。

### SDK

云 API 3.0 提供了配套的开发工具集（SDK），支持多种编程语言，能更方便的调用 API。

* Tencent Cloud SDK 3.0 for Python: [CNB](https://cnb.cool/tencent/cloud/api/sdk/tencentcloud-sdk-python/-/blob/master/tencentcloud/ocr/v20181119/ocr_client.py), [GitHub](https://github.com/TencentCloud/tencentcloud-sdk-python/blob/master/tencentcloud/ocr/v20181119/ocr_client.py), [Gitee](https://gitee.com/TencentCloud/tencentcloud-sdk-python/blob/master/tencentcloud/ocr/v20181119/ocr_client.py)
* Tencent Cloud SDK 3.0 for Java: [CNB](https://cnb.cool/tencent/cloud/api/sdk/tencentcloud-sdk-java/-/blob/master/src/main/java/com/tencentcloudapi/ocr/v20181119/OcrClient.java), [GitHub](https://github.com/TencentCloud/tencentcloud-sdk-java/blob/master/src/main/java/com/tencentcloudapi/ocr/v20181119/OcrClient.java), [Gitee](https://gitee.com/TencentCloud/tencentcloud-sdk-java/blob/master/src/main/java/com/tencentcloudapi/ocr/v20181119/OcrClient.java)
* Tencent Cloud SDK 3.0 for PHP: [CNB](https://cnb.cool/tencent/cloud/api/sdk/tencentcloud-sdk-php/-/blob/master/src/TencentCloud/Ocr/V20181119/OcrClient.php), [GitHub](https://github.com/TencentCloud/tencentcloud-sdk-php/blob/master/src/TencentCloud/Ocr/V20181119/OcrClient.php), [Gitee](https://gitee.com/TencentCloud/tencentcloud-sdk-php/blob/master/src/TencentCloud/Ocr/V20181119/OcrClient.php)
* Tencent Cloud SDK 3.0 for Go: [CNB](https://cnb.cool/tencent/cloud/api/sdk/tencentcloud-sdk-go/-/blob/master/tencentcloud/ocr/v20181119/client.go), [GitHub](https://github.com/TencentCloud/tencentcloud-sdk-go/blob/master/tencentcloud/ocr/v20181119/client.go), [Gitee](https://gitee.com/TencentCloud/tencentcloud-sdk-go/blob/master/tencentcloud/ocr/v20181119/client.go)
* Tencent Cloud SDK 3.0 for Node.js: [CNB](https://cnb.cool/tencent/cloud/api/sdk/tencentcloud-sdk-nodejs/-/blob/master/src/services/ocr/v20181119/ocr_client.ts), [GitHub](https://github.com/TencentCloud/tencentcloud-sdk-nodejs/blob/master/src/services/ocr/v20181119/ocr_client.ts), [Gitee](https://gitee.com/TencentCloud/tencentcloud-sdk-nodejs/blob/master/src/services/ocr/v20181119/ocr_client.ts)
* Tencent Cloud SDK 3.0 for .NET: [CNB](https://cnb.cool/tencent/cloud/api/sdk/tencentcloud-sdk-dotnet/-/blob/master/TencentCloud/Ocr/V20181119/OcrClient.cs), [GitHub](https://github.com/TencentCloud/tencentcloud-sdk-dotnet/blob/master/TencentCloud/Ocr/V20181119/OcrClient.cs), [Gitee](https://gitee.com/TencentCloud/tencentcloud-sdk-dotnet/blob/master/TencentCloud/Ocr/V20181119/OcrClient.cs)
* Tencent Cloud SDK 3.0 for C++: [CNB](https://cnb.cool/tencent/cloud/api/sdk/tencentcloud-sdk-cpp/-/blob/master/ocr/src/v20181119/OcrClient.cpp), [GitHub](https://github.com/TencentCloud/tencentcloud-sdk-cpp/blob/master/ocr/src/v20181119/OcrClient.cpp), [Gitee](https://gitee.com/TencentCloud/tencentcloud-sdk-cpp/blob/master/ocr/src/v20181119/OcrClient.cpp)
* Tencent Cloud SDK 3.0 for Ruby: [CNB](https://cnb.cool/tencent/cloud/api/sdk/tencentcloud-sdk-ruby/-/blob/master/tencentcloud-sdk-ocr/lib/v20181119/client.rb), [GitHub](https://github.com/TencentCloud/tencentcloud-sdk-ruby/blob/master/tencentcloud-sdk-ocr/lib/v20181119/client.rb), [Gitee](https://gitee.com/TencentCloud/tencentcloud-sdk-ruby/blob/master/tencentcloud-sdk-ocr/lib/v20181119/client.rb)

### 命令行工具

* [Tencent Cloud CLI 3.0](https://cloud.tencent.com/document/product/440/6176)

## 6. 错误码

以下仅列出了接口业务逻辑相关的错误码，其他错误码详见 [公共错误码](https://cloud.tencent.com/document/api/866/33528#.E5.85.AC.E5.85.B1.E9.94.99.E8.AF.AF.E7.A0.81)。


| 错误码                                           | 描述                                                                 |
| ------------------------------------------------ | -------------------------------------------------------------------- |
| FailedOperation.CardSideError                    | 身份证CardSide类型错误                                               |
| FailedOperation.DownLoadError                    | 文件下载失败。                                                       |
| FailedOperation.EmptyImageError                  | 图片内容为空。                                                       |
| FailedOperation.IdCardInfoIllegal                | 第二代身份证信息不合法或缺失（身份证号、姓名字段校验非法等）         |
| FailedOperation.IdCardTooSmall                   | 图片分辨率过小或身份证在原图中的占比过小                             |
| FailedOperation.ImageBlur                        | 图片模糊。                                                           |
| FailedOperation.ImageDecodeFailed                | 图片解码失败。                                                       |
| FailedOperation.ImageNoIdCard                    | 图片中未检测到第二代身份证或临时身份证。                             |
| FailedOperation.ImageSizeTooLarge                | 图片尺寸过大，请参考输入参数中关于图片大小限制的说明。               |
| FailedOperation.MultiCardError                   | 图片中存在两张及以上同面卡证，请上传卡证单面图片或一正一反双面图片。 |
| FailedOperation.OcrFailed                        | OCR识别失败。                                                        |
| FailedOperation.UnKnowError                      | 未知错误。                                                           |
| FailedOperation.UnOpenError                      | 服务未开通。                                                         |
| InvalidParameter.ConfigFormatError               | Config不是有效的JSON格式。                                           |
| InvalidParameterValue.InvalidParameterValueLimit | 参数值错误。                                                         |
| LimitExceeded.TooLargeFileError                  | 文件内容太大。                                                       |
| ResourceUnavailable.InArrears                    | 账号已欠费。                                                         |
| ResourceUnavailable.ResourcePackageRunOut        | 账号资源包耗尽。                                                     |
| ResourcesSoldOut.ChargeStatusException           | 计费状态异常。                                                       |
