package com.ccue.cmim.dispatch.onduty.engine;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.hi.SpringContextHolder;
import org.hi.base.enumeration.model.YesNo;
import org.hi.baseservice.BaseServiceHelper;
import org.hi.baseservice.model.ShiftInfo;
import org.hi.common.util.BeanUtil;
import org.hi.framework.dao.Filter;
import org.hi.framework.dao.Sorter;
import org.hi.framework.dao.impl.FilterFactory;
import org.hi.framework.dao.impl.SorterFactory;

import com.ccue.cmim.dispatch.basics.model.DisOnDutyLerder;
import com.ccue.cmim.dispatch.basics.model.DisOnDutyNmber;
import com.ccue.cmim.dispatch.basics.model.DisOndutyRule;
import com.ccue.cmim.dispatch.basics.model.DutyType;
import com.ccue.cmim.dispatch.basics.service.DisOnDutyLerderManager;
import com.ccue.cmim.dispatch.basics.service.DisOnDutyNmberManager;
import com.ccue.cmim.dispatch.basics.service.DisOndutyRuleManager;
import com.ccue.cmim.dispatch.onduty.model.ClassAndOnduty;
import com.ccue.cmim.dispatch.onduty.model.DisLeaderClassOndutyPlan;
import com.ccue.cmim.dispatch.onduty.model.DisLeaderMainOndutyPlan;
import com.ccue.cmim.dispatch.onduty.model.DisLeaderOndutyPlan;
import com.ccue.cmim.dispatch.onduty.model.DisLeaderViceOndutyPlan;
import com.ccue.cmim.dispatch.onduty.service.DisLeaderClassOndutyPlanManager;
import com.ccue.cmim.dispatch.onduty.service.DisLeaderMainOndutyPlanManager;
import com.ccue.cmim.dispatch.onduty.service.DisLeaderOndutyPlanManager;
import com.ccue.cmim.dispatch.onduty.service.DisLeaderViceOndutyPlanManager;

public class SchedulingEngine {
	private static SchedulingEngine engin = new SchedulingEngine();
	private SchedulingEngine(){}
	private List<ISchedulingProcessor> processors;
	
	public static SchedulingEngine getInstance(){
		return  engin;
	}
	
	public List<ISchedulingProcessor> getProcessors(){
//		if(processors == null){
			processors = new LinkedList<ISchedulingProcessor>();
			Filter filter = FilterFactory.getSimpleFilter("startUsing", YesNo.YESNO_YES);
			Sorter sorter = SorterFactory.getSimpleSort("serialNum", Sorter.ORDER_ASC);
			DisOndutyRuleManager dorMgr = (DisOndutyRuleManager)SpringContextHolder.getBean(DisOndutyRule.class);
			List<DisOndutyRule> rList = dorMgr.getObjects(filter, sorter);
			for (DisOndutyRule disOndutyRule : rList) {
				String clzName = disOndutyRule.getPlanClsProsessor();
				processors.add((ISchedulingProcessor)BeanUtil.CreateObject(clzName));
			}
			
//		}
		return processors;
	}
	
	public DisOnDutyNmber getNumber(){
		DisOnDutyNmberManager mgr = (DisOnDutyNmberManager)SpringContextHolder.getBean(DisOnDutyNmber.class);
		DisOnDutyNmber number = (DisOnDutyNmber)mgr.getUniqueObject(FilterFactory.getBlankFilter());
		//缺省为一个副值班，两个带班，带班集合脚标0为主带班
		if(number == null){
			number = new DisOnDutyNmber();
			number.setClassnumer(2);
			number.setDeputyondutyCont(1);
		}
		
		return number;
	}
	
	public void compute(DisLeaderOndutyPlan pl) throws Exception{
		if(pl == null || StringUtils.isBlank(pl.getMonth()))
			return;
		Date startDay = DateUtils.parseDate(pl.getMonth() + "-01", "yyyy-MM-dd");
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(startDay);
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH);
		calendar.set(year, month+1, 1);
		calendar.add(Calendar.DATE, -1);
		Integer daynumInMonth = calendar.get(Calendar.DAY_OF_MONTH);
		//以上只是为了获取当前月份的天数
		calendar.setTime(startDay);
		
		List<DisOnDutyLerder> mainZs = new ArrayList<DisOnDutyLerder>();
		List<DisOnDutyLerder> viceZs = new ArrayList<DisOnDutyLerder>();
		List<DisOnDutyLerder> mainDs = new ArrayList<DisOnDutyLerder>();
		List<DisOnDutyLerder> viceDs = new ArrayList<DisOnDutyLerder>();
		DisOnDutyLerderManager dodlMgr = (DisOnDutyLerderManager)SpringContextHolder.getBean(DisOnDutyLerder.class);
		List<DisOnDutyLerder> all = dodlMgr.getObjects();
		for (DisOnDutyLerder l : all) {
			if((l.getOndutyType() == ClassAndOnduty.CLASSANDONDUTY_CAO_01 || l.getOndutyType() == ClassAndOnduty.CLASSANDONDUTY_CAO_03) &&
					l.getDutytype() == DutyType.DUTYTYPE_DT_01)
				mainZs.add(l);
			if((l.getOndutyType() == ClassAndOnduty.CLASSANDONDUTY_CAO_01 || l.getOndutyType() == ClassAndOnduty.CLASSANDONDUTY_CAO_03)&&
					l.getDutytype() == DutyType.DUTYTYPE_DT_02)
				viceZs.add(l);
			if((l.getOndutyType() == ClassAndOnduty.CLASSANDONDUTY_CAO_01 || l.getOndutyType() == ClassAndOnduty.CLASSANDONDUTY_CAO_02)&&
					l.getDutytype() == DutyType.DUTYTYPE_DT_01)
			if(l.getOndutyType() == ClassAndOnduty.CLASSANDONDUTY_CAO_01 || l.getOndutyType() == ClassAndOnduty.CLASSANDONDUTY_CAO_02)
				mainDs.add(l);
			if((l.getOndutyType() == ClassAndOnduty.CLASSANDONDUTY_CAO_01 || l.getOndutyType() == ClassAndOnduty.CLASSANDONDUTY_CAO_02)&&
					l.getDutytype() == DutyType.DUTYTYPE_DT_02)
				viceDs.add(l);
		}
		
		List<DayArrange> arranges = new LinkedList<DayArrange>();
		List<ShiftInfo> shiftInfoList = BaseServiceHelper.getAllShiftInfo();
		//为一个月的每天添加一个DayArrage对象（日期，主值，副值，三班（map））
		for (int i = 1; i <= daynumInMonth; i++) {
			if(i > 1)
				calendar.add(Calendar.DATE, 1);
			arranges.add(new DayArrange(calendar.getTime(), shiftInfoList));
		}
		
		
		for(ISchedulingProcessor processor : this.getProcessors()){
			arranges = processor.process(pl.getId(), arranges, mainZs, viceZs, mainDs, viceDs);
			System.out.println(processor.getClass().toString());
		}
		
		
		
		
		DisLeaderOndutyPlanManager doMgr = (DisLeaderOndutyPlanManager)SpringContextHolder.getBean(DisLeaderOndutyPlan.class);
		DisLeaderMainOndutyPlanManager mianMgr = (DisLeaderMainOndutyPlanManager)SpringContextHolder.getBean(DisLeaderMainOndutyPlan.class);
		DisLeaderViceOndutyPlanManager viceMgr = (DisLeaderViceOndutyPlanManager)SpringContextHolder.getBean(DisLeaderViceOndutyPlan.class);
		DisLeaderClassOndutyPlanManager laderMgr = (DisLeaderClassOndutyPlanManager)SpringContextHolder.getBean(DisLeaderClassOndutyPlan.class);
		Integer leaOndPlanId = pl.getId();
		doMgr.removeSubs(leaOndPlanId);
		for (DayArrange da : arranges) {
			//保存主值班
			DisLeaderMainOndutyPlan main = new DisLeaderMainOndutyPlan();
			main.setDlvBegin(da.getDay());
			main.setLeaOndPlanId(leaOndPlanId);
			main.setOndutyleader(da.getMain());
			mianMgr.saveObject(main);
			
			for(DisOnDutyLerder l : da.getVices()){
				DisLeaderViceOndutyPlan v = new DisLeaderViceOndutyPlan();
				v.setDlvBegin(da.getDay());
				v.setLeaOndPlanId(leaOndPlanId);
				v.setViceOnduty(l);
				viceMgr.saveObject(v);
			}
			
			for(Map.Entry<ShiftInfo, List<DisOnDutyLerder>> entry : da.getLeads().entrySet()){
				for(DisOnDutyLerder l : entry.getValue()){
					DisLeaderClassOndutyPlan d = new DisLeaderClassOndutyPlan();
					d.setClassLeader(l);
					d.setDlvBegin(da.getDay());
					d.setLeaOndPlanId(leaOndPlanId);
					d.setShiftInfo(entry.getKey());
					laderMgr.saveObject(d);
				}
			}
			
		}
		
	}
	
	
}
