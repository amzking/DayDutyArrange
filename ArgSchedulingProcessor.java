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

public class ArgSchedulingProcessor extends AbstractSchedulingProcessor {
	
	private static Integer DK_TIMES = 6;
	private static Integer FK_TIMES = 9;

	@Override
	public List<DayArrange> process(Integer leaOndPlanId, List<DayArrange> arrange,
			List<DisOnDutyLerder> mzs, List<DisOnDutyLerder> vzs,
			List<DisOnDutyLerder> mds, List<DisOnDutyLerder> vds) {
		
		
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
		
		//zk：满足带班次数,存入List，一个月总计带班=大矿*6 + 副矿 * 9 +副总若干次；
		mdsInMonth = getMDS(arrange.size()*3, mds, otherMDLeaders);
		int shiftInfoNum = mdsInMonth.size();
		
		while(shiftInfoNum != mdCount){
			main.clear();
			DisOnDutyLerder preLeader = new DisOnDutyLerder();
			for(DayArrange da : arrange){
				//主值班
				mzIndex = exeMz(mzAry, main, mzIndex, da);
				
				//主带班 entry.getValue() 返回一个List，第一个就是主带（对应第一个for循环）
				for(Map.Entry<ShiftInfo, List<DisOnDutyLerder>> entry : da.getLeads().entrySet()){
					preLeader = exeMd(mdsInMonth, main, mdIndex, preLeader, da, entry.getValue());
					mdCount++;				
				}				
			}
			
			//zk：每排一个班，就从原list中删除一个，若没有排完则打散重排。
			if(mdsInMonth.size() != 0){
				mdsInMonth.clear();
				mdsInMonth = getMDS(arrange.size()*3, mds, otherMDLeaders);
				for(DayArrange da : arrange){
					for(Map.Entry<ShiftInfo, List<DisOnDutyLerder>> entry : da.getLeads().entrySet()){
						entry.getValue().clear();
					}
				}
				System.out.println("------------------");
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
		
		//一个人不能在同一天出现两次
		Set<DisOnDutyLerder> clacc = new HashSet<DisOnDutyLerder>();
		for (int i = 0; i< arrange.size(); i++) {
			DayArrange da = arrange.get(i);
			clacc.clear();
			clacc.add(da.getMain());
			System.out.println(i + " " + da.getMain().getLeadername().getFullName());
			
			for(DisOnDutyLerder l : da.getVices()){
				if(clacc.contains(l)){
					exchang(arrange, i, 1, da, getType(da, l), l);
				}
				else
					clacc.add(l);
			}
			
			//zk：明确要修改的带班类型，主值换主值，主带还主带
			for(Map.Entry<ShiftInfo, List<DisOnDutyLerder>> entry : da.getLeads().entrySet()){
				for(int j = 0; j < entry.getValue().size(); j++){
					if(j == 0 && clacc.contains(entry.getValue().get(0)) )
						exchang(arrange, i, 1, da, ClassAndOndutyType.MD,entry.getValue().get(j));
					else if(j > 0 && clacc.contains(entry.getValue().get(0)) )
						exchang(arrange, i, 1, da, ClassAndOndutyType.VD,entry.getValue().get(j));
					else
						clacc.add(entry.getValue().get(j));
				}
				
//				for(DisOnDutyLerder l : entry.getValue()){
//					if(clacc.contains(l)){
//						exchang(arrange, i, 1, da, getType(da, l), l);
//					}
//					else
//						clacc.add(l);
//				}				
			}
						
		}
		
		//zk for test
		for (int j = 0; j< arrange.size(); j++) {
			DayArrange datest = arrange.get(j);
			System.out.println(j + " " + datest.getMain().getLeadername().getFullName());
			for(Map.Entry<ShiftInfo, List<DisOnDutyLerder>> entry : datest.getLeads().entrySet()){
				System.out.print(entry.getValue().get(0).getLeadername().getFullName() + " ");			
			}
			System.out.println(" ");
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
	
	
	//zk：排带班，若存在排到最后无法满足：一个人不能连续带两次班，则重排。
	private DisOnDutyLerder exeMd(List<DisOnDutyLerder> mdsInMonth, Map<DisOnDutyLerder, Boolean> main, int mdIndex, DisOnDutyLerder preLeader, DayArrange da, List<DisOnDutyLerder> clacc){
		if(mdIndex == mdsInMonth.size())
			mdIndex = 0;
		
		boolean foundFlag = true;
		for(DisOnDutyLerder l : mdsInMonth){
			if(l.getOndutyType() == ClassAndOnduty.CLASSANDONDUTY_CAO_01 && !l.equals(preLeader)){
				if(main.containsKey(l) && !main.get(l)){
					main.put(l, true);
					foundFlag = false;
				}
				else{
					main.put(l, false);
					foundFlag = true;
					mdsInMonth.remove(l);
					clacc.add(l);
					System.out.println(l.getLeadername().getFullName());
					return l;	
				}
			}
		}
		return preLeader;
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
	
	//zk：获取一个月的排班序列
	List<DisOnDutyLerder> getMDS(int daysInMonth, List<DisOnDutyLerder> mds, List<DisOnDutyLerder> otherMDLeaders){
		List<DisOnDutyLerder> lls = new LinkedList<DisOnDutyLerder>(); 
		for(DisOnDutyLerder leader : mds){
			if(leader.getLeaderLevel() != null){
				if (leader.getLeaderLevel().equals(LeaderLevel.LEADERLEVEL_LL_DK)){
					for(int i = 0; i<DK_TIMES; i++)
						lls.add(leader);
				}
				if(leader.getLeaderLevel().equals(LeaderLevel.LEADERLEVEL_LL_FK)){
					for(int i = 0; i<FK_TIMES; i++)
						lls.add(leader);
				}
				if(leader.getLeaderLevel().equals(LeaderLevel.LEADERLEVEL_LL_FZ)){	
					otherMDLeaders.add(leader);
				}				
			}
		}
		Collections.shuffle(otherMDLeaders);
		int mdOtherIndex = 0;
		while(lls.size() != daysInMonth){
			if(mdOtherIndex == otherMDLeaders.size())
				mdOtherIndex = 0;
			lls.add(otherMDLeaders.get(mdOtherIndex));
			mdOtherIndex++;
		}
		Collections.shuffle(lls);
		return lls;
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
		
		//zk：如果原人是主值，被换的人也应该是主值，反之亦然
		if( sType == ClassAndOndutyType.MZ && tType == ClassAndOndutyType.MD )
			return false;
		if( sType == ClassAndOndutyType.MD && tType == ClassAndOndutyType.MZ )
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
