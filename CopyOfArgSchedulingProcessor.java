package com.ccue.cmim.dispatch.onduty.engine.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hi.baseservice.model.ShiftInfo;

import com.ccue.cmim.dispatch.basics.model.DisOnDutyLerder;
import com.ccue.cmim.dispatch.basics.model.DisOnDutyNmber;
import com.ccue.cmim.dispatch.onduty.engine.ClassAndOndutyType;
import com.ccue.cmim.dispatch.onduty.engine.DayArrange;
import com.ccue.cmim.dispatch.onduty.engine.SchedulingEngine;
import com.ccue.cmim.dispatch.onduty.model.ClassAndOnduty;

public class CopyOfArgSchedulingProcessor extends AbstractSchedulingProcessor {

	@Override
	public List<DayArrange> process(Integer leaOndPlanId, List<DayArrange> arrange,
			List<DisOnDutyLerder> mzs, List<DisOnDutyLerder> vzs,
			List<DisOnDutyLerder> mds, List<DisOnDutyLerder> vds) {
		Collections.shuffle(mzs);
		Collections.shuffle(vzs);
		Collections.shuffle(mds);
		Collections.shuffle(vds);
		DisOnDutyLerder[] mzAry = mzs.toArray(new DisOnDutyLerder[mzs.size()]);
		DisOnDutyLerder[] vzAry = vzs.toArray(new DisOnDutyLerder[vzs.size()]);
		DisOnDutyLerder[] mdAry = mds.toArray(new DisOnDutyLerder[mds.size()]);
		DisOnDutyLerder[] vdAry = vds.toArray(new DisOnDutyLerder[vds.size()]);
		int mzIndex = 0, vzIndex = 0, mdIndex = 0, vdIndex = 0;
		Map<DisOnDutyLerder, Boolean> main = new HashMap<DisOnDutyLerder, Boolean>();
		Map<DisOnDutyLerder, Boolean> vice = new HashMap<DisOnDutyLerder, Boolean>();
		
		DisOnDutyNmber number = SchedulingEngine.getInstance().getNumber();
		for (DayArrange da : arrange) {
			//主值班
			mzIndex = exeMz(mzAry, main, mzIndex, da);
			
			//副值班
			for(int i = 0; i < number.getDeputyondutyCont(); i++){
				vzIndex = exeVz(vzAry, vice, vzIndex, da);
			}
			
			//主带班 entry.getValue() 返回一个List，第一个就是主带（对应第一个for循环）
			for(Map.Entry<ShiftInfo, List<DisOnDutyLerder>> entry : da.getLeads().entrySet()){
				mdIndex = exeMd(mdAry, main, mdIndex, da, entry.getValue());
			}
			
			for(Map.Entry<ShiftInfo, List<DisOnDutyLerder>> entry : da.getLeads().entrySet()){
				for(int i = 1; i < number.getClassnumer(); i++){
					vdIndex = exeVd(vdAry, vice, vdIndex, da, entry.getValue());
				}
			}
			
		}
		
		//一个人不同在同一天出现两次
		Set<DisOnDutyLerder> clacc = new HashSet<DisOnDutyLerder>();
		for (int i = 0; i< arrange.size(); i++) {
			DayArrange da = arrange.get(i);
			clacc.clear();
			clacc.add(da.getMain());
			
			for(DisOnDutyLerder l : da.getVices()){
				if(clacc.contains(l)){
					exchang(arrange, i, 1, da, getType(da, l), l);
				}
				else
					clacc.add(l);
			}
			
			for(Map.Entry<ShiftInfo, List<DisOnDutyLerder>> entry : da.getLeads().entrySet()){
				for(DisOnDutyLerder l : entry.getValue()){
					if(clacc.contains(l)){
						exchang(arrange, i, 1, da, getType(da, l), l);
					}
					else
						clacc.add(l);
				}
			}
		}
		
		return arrange;
	}
	
	private int exeMz(DisOnDutyLerder[] mzAry, Map<DisOnDutyLerder, Boolean> main, int mzIndex, DayArrange da){
		if(mzIndex == mzAry.length)
			mzIndex = 0;
		DisOnDutyLerder mz = mzAry[mzIndex];
		if(mz.getOndutyType() == ClassAndOnduty.CLASSANDONDUTY_CAO_01){
			//如果已经调过一次则跳过,找下一个人
			if(main.containsKey(mz) && !main.get(mz)){
				main.put(mz, true);
				return exeMz(mzAry, main, ++mzIndex, da);
			}
			else
				main.put(mz, false);
		}
		
		da.setMain(mz);
		return ++mzIndex;
	}
	
	private int exeVz(DisOnDutyLerder[] vzAry, Map<DisOnDutyLerder, Boolean> vice, int vzIndex, DayArrange da){
		if(vzIndex == vzAry.length)
			vzIndex = 0;
		DisOnDutyLerder vz = vzAry[vzIndex];
		if(vz.getOndutyType() == ClassAndOnduty.CLASSANDONDUTY_CAO_01){
			//如果已经调过一次则跳过,找下一个人
			if(vice.containsKey(vz) && !vice.get(vz)){
				vice.put(vz, true);
				return exeVz(vzAry, vice, ++vzIndex, da);
			}
			else
				vice.put(vz, false);
		}
		da.getVices().add(vz);
		return ++vzIndex;
	}
	
	
	private int exeMd(DisOnDutyLerder[] mdAry, Map<DisOnDutyLerder, Boolean> main, int mdIndex, DayArrange da, List<DisOnDutyLerder> clacc){
		if(mdIndex == mdAry.length)
			mdIndex = 0;
		DisOnDutyLerder md = mdAry[mdIndex];
		if(md.getOndutyType() == ClassAndOnduty.CLASSANDONDUTY_CAO_01){
			//如果已经调过一次则跳过,找下一个人
			if(main.containsKey(md) && !main.get(md)){
				main.put(md, true);
				return exeMd(mdAry, main, ++mdIndex, da, clacc);
			}
			else
				main.put(md, false);
		}
		
		clacc.add(md);
		return ++mdIndex;
	}
	
	private int exeVd(DisOnDutyLerder[] vdAry, Map<DisOnDutyLerder, Boolean> vice, int vdIndex, DayArrange da, List<DisOnDutyLerder> clacc){
		if(vdIndex == vdAry.length)
			vdIndex = 0;
		DisOnDutyLerder vd = vdAry[vdIndex];
		if(vd.getOndutyType() == ClassAndOnduty.CLASSANDONDUTY_CAO_01){
			//如果已经调过一次则跳过,找下一个人
			if(vice.containsKey(vd) && !vice.get(vd)){
				vice.put(vd, true);
				return exeVd(vdAry, vice, ++vdIndex, da, clacc);
			}
			else
				vice.put(vd, false);
		}
		clacc.add(vd);
		return ++vdIndex;
	}
	
	
	@Override
	public boolean macth(List<DayArrange> arrange, DayArrange so, DayArrange to, ClassAndOndutyType sType, ClassAndOndutyType tType,
			DisOnDutyLerder s, DisOnDutyLerder t) {
		//如果原岗位是主职，目标岗位为副职则不匹配,反之也不匹配
		if((sType == ClassAndOndutyType.MD || sType == ClassAndOndutyType.MZ) && (tType == ClassAndOndutyType.VD || tType == ClassAndOndutyType.VZ))
			return false;
		if((sType == ClassAndOndutyType.VD || sType == ClassAndOndutyType.VZ) && (tType == ClassAndOndutyType.MD || tType == ClassAndOndutyType.MZ))
			return false;
		
		//如果原岗是值班, 而目标人只能带班则不匹配
		if((sType == ClassAndOndutyType.MZ || sType == ClassAndOndutyType.VZ) && t.getDutytype() == ClassAndOnduty.CLASSANDONDUTY_CAO_02)
			return false;
		//如果原岗是带班, 而目标人只能值班则不匹配
		if((sType == ClassAndOndutyType.MD || sType == ClassAndOndutyType.VD) && t.getDutytype() == ClassAndOnduty.CLASSANDONDUTY_CAO_03)
			return false;
		
		//如果源人已在目标班中存在，则不匹配
		if(exit(to, s)) return false;
		//如果目标人已在源班中存在，则不匹配
		if(exit(so, t)) return false;
		
		return true;
	}
	
	@Override
	public String name() {
		return "Average";
	}

	
}
