package com.ccue.cmim.dispatch.onduty.engine.impl;

import java.util.List;
import java.util.Map;

import org.hi.baseservice.model.ShiftInfo;

import com.ccue.cmim.dispatch.basics.model.DisOnDutyLerder;
import com.ccue.cmim.dispatch.onduty.engine.ClassAndOndutyType;
import com.ccue.cmim.dispatch.onduty.engine.DayArrange;
import com.ccue.cmim.dispatch.onduty.engine.ISchedulingProcessor;
import com.ccue.cmim.dispatch.onduty.engine.SchedulingEngine;

public abstract class AbstractSchedulingProcessor implements
		ISchedulingProcessor {

	//zk：判断dl是否在da中存在
	protected boolean exit(DayArrange da, DisOnDutyLerder dl){
		if(da.getMain().equals(dl))
			return true;
		for (DisOnDutyLerder disOnDutyLerder : da.getVices()) {
			if(disOnDutyLerder.equals(dl))
				return true;
		}
		for(Map.Entry<ShiftInfo, List<DisOnDutyLerder>> entry : da.getLeads().entrySet()){
			for (DisOnDutyLerder disOnDutyLerder : entry.getValue()){
				if(disOnDutyLerder.equals(dl))
					return true;
			}
		}
		
		return false;
	}
	
	protected ClassAndOndutyType getType(DayArrange da, DisOnDutyLerder dl){
		if(da.getMain().equals(dl))
			return ClassAndOndutyType.MZ;
		for (DisOnDutyLerder disOnDutyLerder : da.getVices()) {
			if(disOnDutyLerder.equals(dl))
				return ClassAndOndutyType.VZ;
		}
		
		for(Map.Entry<ShiftInfo, List<DisOnDutyLerder>> entry : da.getLeads().entrySet()){
			List<DisOnDutyLerder> dll = entry.getValue();
			for (int i = 0; i < dll.size(); i++){
				DisOnDutyLerder disOnDutyLerder = dll.get(i);
				if(disOnDutyLerder.equals(dl)){
					if(i == 0)
						return ClassAndOndutyType.MD;
					else
					return ClassAndOndutyType.VD;
				}
			}
		}
		
		return null;
	}
	
	//zk：在其中一天的值班中，s为值班人，用t换掉s;zk 0704改
	protected void setDisOnDutyLerder(DayArrange da, DisOnDutyLerder s, ClassAndOndutyType sType, DisOnDutyLerder t){
		if(da.getMain().equals(s) && sType.equals(ClassAndOndutyType.MZ)){
			da.setMain(t);
		}
		List<DisOnDutyLerder> vices = da.getVices();
		for(int i = 0; i < vices.size(); i++){
			DisOnDutyLerder l = vices.get(i);			
			if(l.equals(s)){
				vices.set(i, t);
				break;
			}
		}
		
		//zk：？ 有问题，若存在两个相同的主带，全部被换，还是相同
		for(Map.Entry<ShiftInfo, List<DisOnDutyLerder>> entry : da.getLeads().entrySet()){
			List<DisOnDutyLerder> leads = entry.getValue();
			for(int i = 0; i < leads.size(); i++){
				DisOnDutyLerder l = leads.get(i);	
				if(l.equals(s) && !sType.equals(ClassAndOndutyType.MZ)){
					leads.set(i, t);
					return;
				}
			}
		}
			
	}
	
	protected  int getOffsetIndex(int length, int index, int offset){
		if(offset == 0)
			return index;
		int i = index + offset;
		if(i < 0)
			return length + i;
		if( i > length){
			return i - length;
		}
		
		return i;
	}

	protected Boolean exchang(List<DayArrange> arrange, int index, int time, DayArrange so, ClassAndOndutyType sType, DisOnDutyLerder s){
		if(time > 12)
			return false;
		
		int afterIndex = getOffsetIndex(arrange.size() - 1, index, time);
		int beforIndex = getOffsetIndex(arrange.size() - 1, index, ~time + 1);   //？
		
		DayArrange to = arrange.get(afterIndex);
		if(isSuccess(arrange, to, so, sType, s)) return true;
		
		to = arrange.get(beforIndex);
		if(isSuccess(arrange, to, so, sType, s)) return true;
		
		return exchang(arrange, beforIndex, ++time, so, sType, s);
				
	}
	
	private boolean isSuccess(List<DayArrange> arrange, DayArrange to, DayArrange so, ClassAndOndutyType sType, DisOnDutyLerder s){
		DisOnDutyLerder t = to.getMain();
		if(macths(arrange, so, to, sType, s, t) && sType.equals(ClassAndOndutyType.MZ)){
			to.setMain(s);
			setDisOnDutyLerder(so, s, sType, t);
			return true;
		}
		List<DisOnDutyLerder> vices = to.getVices();
		for (int i = 0; i < vices.size(); i++) {
			t = vices.get(i);
			if(macths(arrange, so, to, sType, s, t)){
				vices.set(i, s);
				setDisOnDutyLerder(so, s, sType, t);
				return true;
			}
		}
		
		for(Map.Entry<ShiftInfo, List<DisOnDutyLerder>> entry : to.getLeads().entrySet()){
			List<DisOnDutyLerder> leads = entry.getValue();
			for (int i = 0; i < leads.size(); i++) {
				t = leads.get(i);
				if(macths(arrange, so, to, sType, s, t)){
					leads.set(i, s);
					setDisOnDutyLerder(so, s, sType, t);
					return true;
				}
			}
		}
		
		return false;
	}
	
	//zk：macths  和 macth 不同
	//zk：相当于不断寻找，直到找到一个，与so变换之后不影响规则的to
	private boolean macths(List<DayArrange> arrange, DayArrange so, DayArrange to,ClassAndOndutyType sType,
			DisOnDutyLerder s, DisOnDutyLerder t){
		ClassAndOndutyType tType = getType(to, t);
		List<ISchedulingProcessor> processors = SchedulingEngine.getInstance().getProcessors();
		for (ISchedulingProcessor sp : processors) {
			System.out.println(sp.name());
			if(!sp.macth(arrange, so, to, sType, tType, s, t))
				return false;
		}
		return true;
	}
}
