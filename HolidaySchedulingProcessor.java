package com.ccue.cmim.dispatch.onduty.engine.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hi.SpringContextHolder;
import org.hi.framework.dao.Filter;
import org.hi.framework.dao.impl.FilterFactory;
import org.hi.baseservice.model.ShiftInfo;

import com.ccue.cmim.dispatch.basics.model.DisOnDutyLerder;
import com.ccue.cmim.dispatch.onduty.engine.ClassAndOndutyType;
import com.ccue.cmim.dispatch.onduty.engine.DayArrange;
import com.ccue.cmim.dispatch.onduty.engine.ISchedulingProcessor;
import com.ccue.cmim.dispatch.onduty.engine.SchedulingEngine;
import com.ccue.cmim.dispatch.onduty.model.DisLeaderVacation;
import com.ccue.cmim.dispatch.onduty.service.DisLeaderVacationManager;

public class HolidaySchedulingProcessor extends AbstractSchedulingProcessor {

	private static ThreadLocal<List<DisLeaderVacation>> thredLocal = new ThreadLocal<List<DisLeaderVacation>>(){
		protected List<DisLeaderVacation> initialValue() {
			return new ArrayList<DisLeaderVacation>(0);
			}
	};
	
	
	@Override
	public List<DayArrange> process(Integer leaOndPlanId, List<DayArrange> arrange,
			List<DisOnDutyLerder> mz, List<DisOnDutyLerder> vz,
			List<DisOnDutyLerder> md, List<DisOnDutyLerder> vd) {
		
		DisLeaderVacationManager dvMgr = (DisLeaderVacationManager)SpringContextHolder.getBean(DisLeaderVacation.class);
		//leanOndPlanId 排班计划主键，对应排班月份：yyyy-mm
		Filter filter = FilterFactory.getSimpleFilter("leaOndPlanId", leaOndPlanId);
		List<DisLeaderVacation> dvs = dvMgr.getObjects(filter);
		System.out.println("-------------------" + Thread.currentThread().getId() + "---------------------\n" + dvs.size());
		thredLocal.set(dvs);
		
		
		for (DisLeaderVacation disLeaderVacation : dvs) {
			Date startDate = disLeaderVacation.getDlvBegin();
			Date endDate = disLeaderVacation.getDlvEnd() == null ? startDate : disLeaderVacation.getDlvEnd();
			DisOnDutyLerder l = disLeaderVacation.getLeaderName();
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(startDate);
			Calendar endCalendar = Calendar.getInstance();
			endCalendar.setTime(endDate);
			//endCalendar.add(Calendar.DATE, -1);
			System.out.println("shuchu------------------------");
			do{
				Date _crrentDate = calendar.getTime();
				for(int i = 0; i < arrange.size(); i++){
					DayArrange da = arrange.get(i);
					//如果请假不是这一天
					if(!da.getDay().equals(_crrentDate))
						continue;
					//如果请假这天不当班
					if(!exit(da, l))
						continue;
					//zk for test
					System.out.println(i+ "before " + da.getMain().getLeadername().getFullName()+ names(da) +" ");
					Boolean ch = exchang(arrange, i, 1, da, getType(da, l), l);
					//test
					System.out.println(i+ "after " + da.getMain().getLeadername().getFullName()+ names(da) +"***" + l.getLeadername().getFullName() + " " +ch.toString());
					Boolean eql = da.getDay().equals(_crrentDate);
					//test
					System.out.println(_crrentDate.toString() + " " + da.getDay().toString() + " " + eql.toString());					
				}
				calendar.add(Calendar.DATE, 1);//增加日期
			}while(calendar.getTime().before(endCalendar.getTime()));
			
			//byzk 0705
//			Date _crrentDate = calendar.getTime();
//			for(int i = 0; i< arrange.size(); i++){
//				DayArrange da = arrange.get(i);
//				if(da.getDay().equals(_crrentDate)){
//					do{
//						exchang(arrange, i, 1, da, getType(da,l),l);
//					}while(da.getDay().before(endCalendar.getTime()));
//				}
//				
//			}
			
		}
		
		//zk for test
		for (int j = 0; j< arrange.size(); j++) {
			DayArrange datest = arrange.get(j);
			System.out.println(j + "h" + datest.getMain().getLeadername().getFullName());
			for(Map.Entry<ShiftInfo, List<DisOnDutyLerder>> entry : datest.getLeads().entrySet()){
				System.out.print(entry.getValue().get(0).getLeadername().getFullName() + " ");			
			}
			System.out.println(" ");
		}
		return arrange;
	}
	
	//zk for test 
	public String names(DayArrange da){
		String str = " ";
		for(Map.Entry<ShiftInfo, List<DisOnDutyLerder>> entry : da.getLeads().entrySet()){
			str += (entry.getValue().get(0).getLeadername().getFullName() + " ");			
		}
		return str;
	}

	
	
	@Override
	public boolean macth(List<DayArrange> arrange, DayArrange so, DayArrange to,
			ClassAndOndutyType sType, ClassAndOndutyType tType,
			DisOnDutyLerder s, DisOnDutyLerder t) {
		System.out.println("test ----- " + Thread.currentThread().getId() + " -------- " + thredLocal.get().size());
		for (DisLeaderVacation dlv : thredLocal.get()) {
			System.out.println("test " + Thread.currentThread().getId() + " --------" + dlv.toString());
			long start = dlv.getDlvBegin().getTime();
			long end = dlv.getDlvEnd() == null ? start : dlv.getDlvEnd().getTime();
			long adTime = so.getDay().getTime();
			//如果目标人在源的那天请假
			if(dlv.getLeaderName().equals(t) && adTime >= start && adTime <= end)
				return false;
			
			//如果源人在目标那天还是请假
			adTime = to.getDay().getTime();
			if(dlv.getLeaderName().equals(s) && adTime >= start && adTime <= end)
				return false;
			System.out.println(start + " ," + end + "," + adTime);
			//zk: 永远都不要和在自己请假期间的人还，即使他不请假，但那天你还在请假，找个假期外的人换吧。
			if(adTime >= start && adTime <=end){
				return false;
			}
			
			
		}
		
		System.out.println("fanhuitrue");
		return true;
	}

	@Override
	public String name() {
		return "Holiday";
	}
	

}
