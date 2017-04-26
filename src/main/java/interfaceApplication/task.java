package interfaceApplication;

import java.util.HashMap;

import org.json.simple.JSONObject;

import esayhelper.JSONHelper;
import esayhelper.TimeHelper;
import model.taskModel;

public class task {
	private taskModel model = new taskModel();
	private HashMap<String, Object> map = new HashMap<>();
	private JSONObject _obj = new JSONObject();

//	private static int userPlv;
//	static{
//		userPlv = Integer.parseInt(execRequest._run("GrapeAuth/Auth/getUserPlv", null).toString());
//	}
	
	public task() {
		map.put("timediff", 3); // 更新时间周期，单位为天
		map.put("lasttime", String.valueOf(TimeHelper.nowMillis()));
		map.put("state", 0); //0：未开始；1：进行中；2：已结束；3：已取消
		map.put("type", 0);
		map.put("ownid", 0);  //所有者id
		map.put("priority", 0);  //任务优先级  
	}

	@SuppressWarnings("unchecked")
	public String TaskAdd(String info) {
//		String code = execRequest._run("GrapeAuth/Auth/InsertPLV", null).toString();
//		if (!"0".equals(code)) {
//			return model.resultMessage(2, "");
//		}
		JSONObject object = model.AddMap(map, JSONHelper.string2json(info));
		_obj.put("records", JSONHelper.string2json(model.Add(object)));
		return model.resultMessage(0, _obj.toString());
	}

	public String TaskUpdate(String id, String info) {
//		int uplv = Integer.parseInt(model.find(id).get("uPlv").toString());
//		if (userPlv<uplv) {
//			return model.resultMessage(3, "");
//		}
		return model.resultMessage(model.update(id, JSONHelper.string2json(info)), "任务修改成功");
	}

	public String TaskDelete(String id) {
//		int dplv = Integer.parseInt(model.find(id).get("dPlv").toString());
//		if (userPlv<dplv) {
//			return model.resultMessage(4, "");
//		}
		return model.resultMessage(model.delete(id), "任务删除成功");
	}

	public String TaskBatchDelete(String ids) {
		return model.resultMessage(model.delete(ids.split(",")), "任务批量删除成功");
	}

	@SuppressWarnings("unchecked")
	public String TaskPage(int ids, int pageSize) {
		_obj.put("records", model.page(ids, pageSize));
		return model.resultMessage(0, _obj.toString());
	}

	@SuppressWarnings("unchecked")
	public String TaskPageBy(int ids, int pageSize, String info) {
		_obj.put("records", model.page(ids, pageSize, JSONHelper.string2json(info)));
		return model.resultMessage(0, _obj.toString());
	}

	// 需要通知栏目所有人更新栏目
	@SuppressWarnings("unchecked")
	public String TaskNotice(String username) {
		_obj.put("records", model.notice(username));
		return model.resultMessage(0, _obj.toString());
	}
}
