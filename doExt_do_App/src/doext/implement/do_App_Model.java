package doext.implement;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.text.TextUtils;
import core.DoServiceContainer;
import core.helper.DoIOHelper;
import core.helper.DoJsonHelper;
import core.interfaces.DoIDataFS;
import core.interfaces.DoIInitDataFS;
import core.interfaces.DoIScriptEngine;
import core.interfaces.DoISourceFS;
import core.object.DoInvokeResult;
import core.object.DoMultitonModule;
import core.object.DoSingletonModule;
import core.object.DoSourceFile;
import doext.define.do_App_IMethod;

/**
 * 自定义扩展SM组件Model实现，继承DoSingletonModule抽象类，并实现do_App_IMethod接口方法；
 * #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象； 获取DoInvokeResult对象方式new
 * DoInvokeResult(this.getUniqueKey());
 */
public class do_App_Model extends DoSingletonModule implements do_App_IMethod {

	public do_App_Model(String _appID) throws Exception {
		super();
		this.appID = _appID;
	}

	@Override
	public void loadApp(String _appID) throws Exception {
		if (!DoIOHelper.existDirectory(DoServiceContainer.getGlobal().getSourceRootPath() + "/" + _appID)) {
			throw new Exception("不存在应用：" + _appID);
		}
		this.appID = _appID;

		// 初始化成员变量
		this.dataFS = DoServiceContainer.getDataFS();
		this.sourceFS = DoServiceContainer.getSourceFS();
		this.initDataFS = DoServiceContainer.getInitDataFS();
		this.scriptEngine = DoServiceContainer.getScriptEngineFactory().createScriptEngine(this, null, null, getScriptsName());
		this.dictMultitonModuleIDs = new HashMap<String, String>();
		this.dictMultitonModuleAddresses = new HashMap<String, DoMultitonModule>();
		if (this.scriptEngine == null) {
			throw new Exception("无法创建脚本引擎");
		}
	}

	@Override
	public void loadScripts() throws Exception {
		DoSourceFile _scriptFile = this.getSourceFS().getSourceByFileName("source://" + getScriptsName());
		if (_scriptFile != null && _scriptFile.getTxtContent().length() > 0) {
			this.scriptEngine.loadScripts(_scriptFile.getTxtContent());
		}
	}

	private String getScriptsName() {
		String startMainApp = "app.js";
		if (".lua".equals(DoServiceContainer.getGlobal().getScriptType())) {
			startMainApp = "app.lua";
		}
		return startMainApp;
	}

	@Override
	public void dispose() {
		super.dispose();
		if (this.dataFS != null) {
			this.dataFS.dispose();
			this.dataFS = null;
		}
		if (this.initDataFS != null) {
			this.initDataFS.dispose();
			this.initDataFS = null;
		}
		if (this.sourceFS != null) {
			this.sourceFS.dispose();
			this.sourceFS = null;
		}
		if (this.scriptEngine != null) {
			this.scriptEngine.dispose();
			this.scriptEngine = null;
		}
		if (this.dictMultitonModuleIDs != null) {
			this.dictMultitonModuleIDs.clear();
			this.dictMultitonModuleIDs = null;
		}
		if (this.dictMultitonModuleAddresses != null) {
			// 释放每一个子Model
			for (DoMultitonModule _multitonModule : this.dictMultitonModuleAddresses.values()) {
				_multitonModule.dispose();
			}
			this.dictMultitonModuleAddresses.clear();
			this.dictMultitonModuleAddresses = null;
		}

	}

	private String appID;
	private DoIDataFS dataFS;
	private DoIInitDataFS initDataFS;
	private DoISourceFS sourceFS;
	private DoIScriptEngine scriptEngine;

	@Override
	public String getAppID() {
		return this.appID;
	}

	@Override
	public DoIDataFS getDataFS() {
		return this.dataFS;
	}

	@Override
	public DoIInitDataFS getInitDataFS() {
		return this.initDataFS;
	}

	@Override
	public DoISourceFS getSourceFS() {
		return this.sourceFS;
	}

	public DoIScriptEngine getScriptEngine() {
		return this.scriptEngine;
	}

	private Map<String, DoMultitonModule> dictMultitonModuleAddresses;

	@Override
	public DoMultitonModule createMultitonModule(String _typeID, String _id) throws Exception {
		if (_typeID == null || _typeID.length() <= 0)
			throw new Exception("未指定Model组件的type值");
		DoMultitonModule _multitonModule = null;
		if (_id != null && _id.length() > 0) {
			String _tempID = _typeID + _id;
			String _address = this.dictMultitonModuleIDs.get(_tempID);
			if (_address != null) {
				_multitonModule = this.dictMultitonModuleAddresses.get(_address);
			}
		}

		if (_multitonModule == null) {
			_multitonModule = DoServiceContainer.getMultitonModuleFactory().createMultitonModule(_typeID);
			if (_multitonModule == null)
				throw new Exception("遇到无效的Model组件：" + _typeID);
			_multitonModule.setCurrentPage(null);
			_multitonModule.setCurrentApp(this);
			this.dictMultitonModuleAddresses.put(_multitonModule.getUniqueKey(), _multitonModule);
			if (_id != null && _id.length() > 0) {
				String _tempID = _typeID + _id;
				this.dictMultitonModuleIDs.put(_tempID, _multitonModule.getUniqueKey());
			}
		}
		return _multitonModule;
	}

	@Override
	public boolean deleteMultitonModule(String _address) {
		DoMultitonModule _multitonModule = this.getMultitonModuleByAddress(_address);
		if (_multitonModule == null)
			return false;
		_multitonModule.dispose();
		this.dictMultitonModuleAddresses.remove(_address);
		for (String key : this.dictMultitonModuleIDs.keySet()) {
			if (_address.equals(this.dictMultitonModuleIDs.get(key))) {
				this.dictMultitonModuleIDs.remove(key);
				break;
			}
		}
		return true;
	}

	public DoMultitonModule getMultitonModuleByAddress(String _key) {
		if (!this.dictMultitonModuleAddresses.containsKey(_key))
			return null;
		return this.dictMultitonModuleAddresses.get(_key);
	}

	// 处理成员方法
	@Override
	public boolean invokeSyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		if ("getAppID".equals(_methodName)) { // 获取应用ID
			this.getAppID(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		return super.invokeSyncMethod(_methodName, _dictParas, _scriptEngine, _invokeResult);
	}

	@Override
	public boolean invokeAsyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		if ("openPage".equals(_methodName)) { // 打开一个页面
			this.openPage(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		} else if ("closePage".equals(_methodName)) {// 关闭当前页面
			this.closePage(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		} else if ("closePageToID".equals(_methodName)) {// 关闭当前页面
			this.closePageToID(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		} else if ("update".equals(_methodName)) {
			this.update(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		}
		return super.invokeAsyncMethod(_methodName, _dictParas, _scriptEngine, _callbackFuncName);
	}

	@Override
	public void update(JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) {
		DoInvokeResult _invokeResult = new DoInvokeResult(this.getUniqueKey());
		try {
			JSONArray _sources = DoJsonHelper.getJSONArray(_dictParas, "source");
			String _target = DoJsonHelper.getString(_dictParas, "target", "");
			if (_sources != null && _sources.length() > 0 && !TextUtils.isEmpty(_target)) {
				// 只允许是source目录，如果目录不存在，则创建对应的目录
				if (!_target.startsWith(DoISourceFS.SOURCE_PREFIX)) {
					_invokeResult.setResultBoolean(false);
					throw new Exception("target只允许是" + DoISourceFS.SOURCE_PREFIX + "打头!");
				}
				//检查source里面是否包含了不合法目录(不支持source://开头的目录)
				if (!DoIOHelper.checkFilePathValidate(_sources)) {
					throw new Exception("source参数只支持" + DoISourceFS.DATA_PREFIX + " 打头!");
				}
				//这个必须是source对应的mappingSource路径
				String _related_url = _target.substring(DoISourceFS.SOURCE_PREFIX.length());
				_target = _scriptEngine.getCurrentApp().getSourceFS().getMappingSourceRootPath() + File.separator + _related_url;
				if (!DoIOHelper.existFileOrDirectory(_target)) {
					DoIOHelper.createDirectory(_target);
				}
				for (int i = 0; i < _sources.length(); i++) {
					String _fullPath = DoIOHelper.getLocalFileFullPath(_scriptEngine.getCurrentApp(), _sources.getString(i));
					DoIOHelper.copyFileOrDirectory(_fullPath, _target);
				}
			}
			_invokeResult.setResultBoolean(true);
		} catch (Exception e) {
			_invokeResult.setResultBoolean(false);
			DoServiceContainer.getLogEngine().writeError("DoApp update /t", e);
		} finally {
			// 清除source文件字典
			this.sourceFS.clear();
			_scriptEngine.callback(_callbackFuncName, _invokeResult);
		}
	}

	@Override
	public void getAppID(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) {
		_invokeResult.setResultText(this.getAppID());
	}

	private Map<String, String> dictMultitonModuleIDs;

	@Override
	public void openPage(JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {

		String _pageID = DoJsonHelper.getString(_dictParas, "id", null);
		String _pageFile = DoJsonHelper.getString(_dictParas, "source", null);
		if (_pageFile == null || _pageFile.length() <= 0)
			throw new Exception("打开页面时未指定相关文件");
		String _animationType = DoJsonHelper.getString(_dictParas, "animationType", "");
		String _scriptType = DoJsonHelper.getString(_dictParas, "scriptType", "");
		String _data = DoJsonHelper.getString(_dictParas, "data", "");
		String _statusBarState = DoJsonHelper.getString(_dictParas, "statusBarState", "show");
		String _statusBarBgColor = DoJsonHelper.getString(_dictParas, "statusBarBgColor", "000000FF");
		String _keyboardMode = DoJsonHelper.getString(_dictParas, "keyboardMode", "hidden");
		DoServiceContainer.getPageViewFactory().openPage(_pageID, this.getAppID(), _pageFile, _scriptType, _animationType, _data, _statusBarState, _statusBarBgColor, _keyboardMode, _callbackFuncName);
	}

	@Override
	public void closePage(JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		String _animationType = DoJsonHelper.getString(_dictParas, "animationType", "");
		String _data = DoJsonHelper.getString(_dictParas, "data", "");
		int _layer = DoJsonHelper.getInt(_dictParas, "layer", 1);
		if (_layer < 1) {
			_layer = 1;
		}
		DoServiceContainer.getPageViewFactory().closePage(_animationType, _data, _layer);
	}

	@Override
	public void closePageToID(JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		String _pageId = DoJsonHelper.getString(_dictParas, "id", null);
		String _animationType = DoJsonHelper.getString(_dictParas, "animationType", "");
		String _data = DoJsonHelper.getString(_dictParas, "data", "");
		if (_pageId == null || _pageId.trim().length() == 0) {
			DoServiceContainer.getPageViewFactory().closePage(_animationType, _data, 1);
		} else {
			DoServiceContainer.getPageViewFactory().closePage(_animationType, _data, _pageId);
		}
	}

	@Override
	public void fireEvent(String _eventName, Object _result) {
		DoInvokeResult _invokeResult = new DoInvokeResult(this.getUniqueKey());
		if (_result != null) {
			if (_result instanceof JSONObject) {
				_invokeResult.setResultNode((JSONObject) _result);
			} else {
				_invokeResult.setResultText(_result.toString());
			}
		}
		this.getEventCenter().fireEvent(_eventName, _invokeResult);
	}

}