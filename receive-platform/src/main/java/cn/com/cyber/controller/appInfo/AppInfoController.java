package cn.com.cyber.controller.appInfo;

import cn.com.cyber.controller.BaseController;
import cn.com.cyber.model.AppInfo;
import cn.com.cyber.model.DeveloperValid;
import cn.com.cyber.model.TreeModel;
import cn.com.cyber.service.AppInfoService;
import cn.com.cyber.service.DeveloperValidService;
import cn.com.cyber.util.CodeUtil;
import cn.com.cyber.util.HttpConnection;
import cn.com.cyber.util.MessageCodeUtil;
import cn.com.cyber.util.common.RestResponse;
import cn.com.cyber.util.common.ResultData;
import cn.com.cyber.util.exception.ValueRuntimeException;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 应用程序信息的控制器
 *
 * @author chenchen
 * @date 2020/02/18
 */
@Controller
@RequestMapping("/appInfo")
public class AppInfoController extends BaseController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppInfoController.class);

    @Autowired
    private AppInfoService appInfoService;

    @Autowired
    private DeveloperValidService developerValidService;

    @Autowired
    private Environment environment;

    @Autowired
    private MessageCodeUtil messageCodeUtil;

    /**
     * 获取应用信息列表
     *
     * @param model 模型
     * @param state 状态
     * @return {@link String}
     */
    @RequestMapping("list")
    public String getAppInfoList(Model model, @RequestParam(value = "state", defaultValue = "1") int state) {
        model.addAttribute("state", state);
        return "appInfo/appInfoList";
    }

    @RequestMapping("peoples")
    public String getAppInfoPeoples() {
        return "peoples/peoples";
    }

    @RequestMapping("peoples/approve")
    public String getAppInfoPeoplesApprove() {
        return "peoples/peoplesApprove";
    }

    //获取应用列表数据
    @RequestMapping("queryAppInfoListData")
    @ResponseBody
    public RestResponse queryAppInfoListData(AppInfo appInfo) {
        RestResponse rest = new RestResponse();
        int code = CodeUtil.BASE_SUCCESS;
        try {
            if (getShiroUser().source == 1) {
                appInfo.setCreator(getShiroUser().id);
            }
            PageHelper.startPage(appInfo.getPageNum(), appInfo.getPageSize());
            List<AppInfo> appInfos = appInfoService.getAppInfoList(appInfo);
            PageInfo<AppInfo> appInfoPage = new PageInfo<AppInfo>(appInfos);
            rest.setData(appInfos).setTotal(appInfoPage.getTotal()).setPage(appInfoPage.getLastPage());
        } catch (ValueRuntimeException e) {
            code = (Integer) e.getValue();
        }
        rest.setCode(code);
        rest.setMessage(messageCodeUtil.getMessage(code));
        return rest;
    }

    @PostMapping(value = "saveAppInfo")
    @ResponseBody
    public RestResponse saveAppInfo(@Valid AppInfo appInfo) {
        int code = CodeUtil.BASE_SUCCESS;
        try {
            appInfoService.saveAppInfo(getShiroUser().id, appInfo);
        } catch (ValueRuntimeException e) {
            code = (Integer) e.getValue();
        }
        return RestResponse.res(code, messageCodeUtil.getMessage(code));
    }

    //统计应用数量
    @RequestMapping("queryCountAppInfo")
    @ResponseBody
    public RestResponse queryCountAppInfo() {
        RestResponse rest = new RestResponse();
        int code = CodeUtil.BASE_SUCCESS;
        try {
            long companyId = getShiroUser().id;   //用户id
            int enable = appInfoService.getCountAppInfoByState(companyId, CodeUtil.APP_STATE_ENABLE);
            int register = appInfoService.getCountAppInfoByState(companyId, CodeUtil.APP_STATE_REGISTER);
            int refuse = appInfoService.getCountAppInfoByState(companyId, CodeUtil.APP_STATE_REFUSE);
            int cancel = appInfoService.getCountAppInfoByState(companyId, CodeUtil.APP_STATE_CANCEL);
            Integer[] data = new Integer[]{enable, register, refuse, cancel};
            rest.setData(data);
        } catch (ValueRuntimeException e) {
            code = (Integer) e.getValue();
        }
        rest.setCode(code);
        rest.setMessage(messageCodeUtil.getMessage(code));
        return rest;
    }

    //获取应用列表数据-接口列表展示
    @RequestMapping("queryAppList")
    @ResponseBody
    public RestResponse queryAppList() {
        RestResponse rest = new RestResponse();
        int code = CodeUtil.BASE_SUCCESS;
        Long companyId = null;
        try {
            if (getShiroUser().source == 1) {
                companyId = getShiroUser().id;
            }
            List<TreeModel> appListTree = appInfoService.getAppListTree(companyId, 1);
            rest.setData(appListTree);
        } catch (ValueRuntimeException e) {
            code = (Integer) e.getValue();
        }
        rest.setCode(code);
        rest.setMessage(messageCodeUtil.getMessage(code));
        return rest;
    }

    //查询用户可见的应用及接口
    @RequestMapping("appServiceTree")
    @ResponseBody
    public RestResponse getAppServiceTree(Integer area) {
        int code = CodeUtil.BASE_SUCCESS;
        RestResponse rest = new RestResponse();
        String companyId = null;
//        if (getShiroUser().source == 1) {
//            companyId = getShiroUser().id;
//        }
        if (area == 0) {
            rest.setData(appInfoService.getAppServiceTree(companyId));
            rest.setCode(code).setMessage(messageCodeUtil.getMessage(code));
        } else {
            DeveloperValid developerValid = new DeveloperValid();
            developerValid.setUserId(getShiroUser().userId);
            List<DeveloperValid> developerValidList = developerValidService.getDeveloperValidList(developerValid);
            if (CollectionUtils.isEmpty(developerValidList)) {
                code = CodeUtil.USERINFO_NULL_DEVELOPER_BIND;
                rest.setCode(code).setMessage(messageCodeUtil.getMessage(code));
                return rest;
            }
            List<Integer> companyIds = Lists.newArrayList();
            List<String> companyParam = Lists.newArrayList();
            for (DeveloperValid valid : developerValidList) {
                companyIds.add(valid.getCompanyId());
                companyParam.add(valid.getUserName());
            }
            Map<String, String> header = Maps.newHashMap();
            header.put("userId", developerValid.getUserId());
            header.put("token", developerValidList.get(0).getToken());
            String url = environment.getProperty(CodeUtil.DEVELOPER_VALID_URL) + CodeUtil.API_SERVICETREE_URL + "?companyIds=" + StringUtils.join(companyIds.toArray(), ",");
            ResultData resultData = HttpConnection.httpRequest(url, CodeUtil.METHOD_GET, null, null, null, header);
            if (resultData != null && CodeUtil.HTTP_OK == resultData.getCode()) {
                JSONObject jsonObject = JSONObject.parseObject(resultData.getResult());
                if (jsonObject.getInteger("code") == 0) {
                    rest.setData(JSONArray.parseArray(jsonObject.getString("data")));
                    rest.setAny("companyParam", companyParam);
                } else {
                    code = CodeUtil.APPSERVICE_ERR_OPTION;
                }
            } else if (resultData != null && CodeUtil.HTTP_VALID_ERR == resultData.getCode()) {
                developerValid.setCompanyKey(developerValidList.get(0).getCompanyKey());
                DeveloperValid valid = developerValidService.validLogin(developerValid);
                header.put("token", valid.getToken());
                resultData = HttpConnection.httpRequest(url, CodeUtil.METHOD_GET, null, null, null, header);
                if (resultData != null && CodeUtil.HTTP_OK == resultData.getCode()) {
                    JSONObject jsonObject = JSONObject.parseObject(resultData.getResult());
                    if (jsonObject.getInteger("code") == 0) {
                        rest.setData(JSONArray.parseArray(jsonObject.getString("data")));
                        rest.setAny("companyParam", companyParam);
                    } else {
                        code = CodeUtil.APPSERVICE_ERR_OPTION;
                    }
                }
            } else {
                LOGGER.error("resultData:{}", resultData.getResult());
                rest.setData(resultData.getResult());
            }
        }
        rest.setCode(code).setMessage(messageCodeUtil.getMessage(code));
        return rest;
    }
}
