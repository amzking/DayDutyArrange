package com.ccue.cmim.dispatch.onduty.engine.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hi.baseservice.model.ShiftInfo;

import com.ccue.cmim.dispatch.basics.model.DisOnDutyLerder;
import com.ccue.cmim.dispatch.basics.model.DisOnDutyNmber;
import com.ccue.cmim.dispatch.basics.model.LeaderLevel;
import com.ccue.cmim.dispatch.onduty.engine.ClassAndOndutyType;
import com.ccue.cmim.dispatch.onduty.engine.DayArrange;
import com.ccue.cmim.dispatch.onduty.engine.SchedulingEngine;
import com.ccue.cmim.dispatch.onduty.model.ClassAndOnduty;

/**
 * @author zk
 *
 */
public class CopyOfLeaderDutyDaysProcessor extends AbstractSchedulingProcessor {

	private HashSet<DisOnDutyLerder> viceBossSet;
	private static Integer DK_TIMES = 6;
	private static Integer FK_TIMES = 9;

	@Override
	public List<DayArrange> process(Integer leaOndPlanId,
			List<DayArrange> arrange, List<DisOnDutyLerder> mzs,
			List<DisOnDutyLerder> vzs, List<DisOnDutyLerder> mds,
			List<DisOnDutyLerder> vds) {
		
		Collections.shuffle(mzs);
		Collections.shuffle(vzs);
		Collections.shuffle(vds);
		DisOnDutyLerder[] mzAry = mzs.toArray(new DisOnDutyLerder[mzs.size()]);
		DisOnDutyLerder[] vzAry = vzs.toArray(new DisOnDutyLerder[vzs.size()]);
		DisOnDutyLerder[] vdAry = vds.toArray(new DisOnDutyLerder[vds.size()]);
		
		int mzIndex = 0, vzIndex = 0, mdIndex = 0, vdIndex = 0, mdCount=0 ;
		Map<DisOnDutyLerder, Boolean> main = new HashMap<DisOnDutyLerder, Boolean>();
		Map<DisOnDutyLerder, Boolean> vice = new HashMap<DisOnDutyLerder, Boolean>();
		List<DisOnDutyLerder> mdsInMonth = new LinkedList<DisOnDutyLerder>();
		List<DisOnDutyLerder> otherMDLeaders = new LinkedList<DisOnDutyLerder>();
		
		//满足带班次数,存入List，一个月总计带班=大矿*6 + 副矿 * 9 +副总若干次；
		getMDS(arrange.size()*3,mds,mdsInMonth,otherMDLeaders);
		int shiftInfoNum = mdsInMonth.size();
		
		while(shiftInfoNum != mdCount){
			for(DayArrange da : arrange){
				//主值班
				mzIndex = exeMz(mzAry, main, mzIndex, da);
				
				//主带班 entry.getValue() 返回一个List，第一个就是主带（对应第一个for循环）
				for(Map.Entry<ShiftInfo, List<DisOnDutyLerder>> entry : da.getLeads().entrySet()){
					exeMd(mdsInMonth, main, mdIndex, da, entry.getValue());
					mdCount++;				
				}				
			}
			
			//每排一个班，就从原list中删除一个，若没有排完则打散重排。
			if(mdsInMonth.size() != 0){
				getMDS(arrange.size()*3,mds,mdsInMonth,otherMDLeaders);
				mdCount = 0;
			}
		}
		
		DisOnDutyNmber number = SchedulingEngine.getInstance().getNumber();
		for (DayArrange da : arrange) {
			//副值班
			for(int i = 0; i < number.getDeputyondutyCont(); i++){
				vzIndex = exeVz(vzAry, vice, vzIndex, da);
			}
			
			//副带班
			for(Map.Entry<ShiftInfo, List<DisOnDutyLerder>> entry : da.getLeads().entrySet()){
				for(int i = 1; i < number.getClassnumer(); i++){
					vdIndex = exeVd(vdAry, vice, vdIndex, da, entry.getValue());
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
	
	//排带班，若存在排到最后无法满足：一个人不能连续带两次班，则重排。
	private void exeMd(List<DisOnDutyLerder> mdsInMonth, Map<DisOnDutyLerder, Boolean> main, int mdIndex, DayArrange da, List<DisOnDutyLerder> clacc){
		if(mdIndex == mdsInMonth.size())
			mdIndex = 0;
		boolean foundFlag = true;
		for(DisOnDutyLerder l : mdsInMonth){
			if(l.getOndutyType() == ClassAndOnduty.CLASSANDONDUTY_CAO_01){
				if(main.containsKey(l) && !main.get(l)){
					main.put(l, true);
					foundFlag = false;
				}
				else{
					main.put(l, false);
					foundFlag = true;
					clacc.add(l);
					mdsInMonth.remove(l);
				}
			
			}
		}
		if(!foundFlag){
			return;
		}
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
	
	
	
	void getMDS(int daysInMonth, List<DisOnDutyLerder> mds, List<DisOnDutyLerder> mdsInMonth, List<DisOnDutyLerder> otherMDLeaders){
		for(DisOnDutyLerder leader : mds){
			if (leader.getLeaderLevel() == LeaderLevel.LEADERLEVEL_LL_DK){
				for(int i = 0; i<DK_TIMES; i++)
					mdsInMonth.add(leader);
			}
			if(leader.getLeaderLevel() == LeaderLevel.LEADERLEVEL_LL_FK ){
				for(int i = 0; i<FK_TIMES; i++)
					mdsInMonth.add(leader);
			}
			if(leader.getLeaderLevel() != LeaderLevel.LEADERLEVEL_LL_DK || leader.getLeaderLevel() != LeaderLevel.LEADERLEVEL_LL_FK){	
				otherMDLeaders.add(leader);
			}
		}
		Collections.shuffle(otherMDLeaders);
		int mdOtherIndex = 0;
		while(mdsInMonth.size() != daysInMonth){
			if(mdOtherIndex == otherMDLeaders.size())
				mdOtherIndex = 0;
			mdsInMonth.add(otherMDLeaders.get(mdOtherIndex));
			mdOtherIndex++;
		}
		Collections.shuffle(mdsInMonth);
		return;
	}
		
		


	/**
	 * 统计大矿，副矿，副总各带班（主）次数
	 * 
	 * @param arrange
	 * @param mainChief
	 */
	private void dutyTimesCaculate(List<DayArrange> arrange,
			Map<DisOnDutyLerder, Integer> mainChief,
			Map<DisOnDutyLerder, Integer> viceChiefs,
			Map<DisOnDutyLerder, Integer> viceBosses) {

		for (DayArrange da : arrange) {
			Map<ShiftInfo, List<DisOnDutyLerder>> dutyLeaders = da.getLeads();
			for (Map.Entry<ShiftInfo, List<DisOnDutyLerder>> sild : dutyLeaders
					.entrySet()) {
				if (sild.getValue().size() > 0) {
					DisOnDutyLerder leader = sild.getValue().get(0);
					if (leader.getLeaderLevel() == LeaderLevel.LEADERLEVEL_LL_DK)
						if (!mainChief.containsKey(leader)) {
							mainChief.put(leader, 1);
						} else {
							mainChief.put(leader, mainChief.get(leader) + 1);
						}
					if (leader.getLeaderLevel() == LeaderLevel.LEADERLEVEL_LL_FK)
						if (!viceChiefs.containsKey(leader)) {
							viceChiefs.put(leader, 1);
						} else {
							viceChiefs.put(leader, viceChiefs.get(leader) + 1);
						}
					if (leader.getLeaderLevel() == LeaderLevel.LEADERLEVEL_LL_FZ) {
						viceBossSet = new HashSet<DisOnDutyLerder>();
						viceBossSet.add(leader);
						if (!viceBosses.containsKey(leader)) {
							viceBosses.put(leader, 1);
						} else {
							viceBosses.put(leader, viceBosses.get(leader) + 1);
						}
					}
				}
			}
		}
	}

	/**
	 * 建立规则 判断是否满足换后，领导主带班次数6次，9次
	 * ，10次-11次，先判断大矿是否是6次，若否，与副总换，再判断每个副矿是否是9次，否与副总换。 一个月的总的带班次数，主带：3*30=90
	 * 6*1+9*4+ 10(or 11)*5 =92~97
	 */
	@Override
	public boolean macth(List<DayArrange> arrange, DayArrange so, DayArrange to,
			ClassAndOndutyType sType, ClassAndOndutyType tType,
			DisOnDutyLerder s, DisOnDutyLerder t) {
		Map<DisOnDutyLerder, Integer> mainChief = new HashMap<DisOnDutyLerder, Integer>();
		Map<DisOnDutyLerder, Integer> viceChiefs = new HashMap<DisOnDutyLerder, Integer>();
		Map<DisOnDutyLerder, Integer> viceBosses = new HashMap<DisOnDutyLerder, Integer>();
		dutyTimesCaculate(arrange, mainChief, viceChiefs, viceBosses);
		
		boolean isDKSatisfied = false;
		boolean isFKSatisfied = true;
		for(Map.Entry<DisOnDutyLerder, Integer> mc : mainChief.entrySet()){
			if (mc.getValue() == 6 ) {
				isDKSatisfied = true;
			}
		}
		for(Map.Entry<DisOnDutyLerder, Integer> vc : viceChiefs.entrySet()){
			if(vc.getValue() != 9)
				isFKSatisfied = false;
		}
		if(isDKSatisfied && isFKSatisfied)
			return true;
		return false;
	}

	@Override
	public String name() {
		return "LeaderDutyDays";
	}

}
