import com.example.horsegenetics.common.parts.*;
import java.util.*;
public class Check { public static void main(String[] a){
 int bad=0;
 for(PartKind k:PartKind.values()) for(double s=0;s<=1.0001;s+=0.25) for(long seed=0;seed<500;seed++){
   List<PartNode> n=PartGenerators.build(k,s,seed);
   if(!n.equals(PartGenerators.build(k,s,seed)))bad++;
   for(int i=0;i<n.size();i++){PartNode p=n.get(i); if(p.parent()>=i)bad++; if(p.girth()<0.49f||p.len()<=0)bad++; if(p.tex()<0||p.tex()>5)bad++;}
   if(k==PartKind.ANTLERS&&n.size()>AntlerGenerator.MAX_NODES)bad++;
 }
 float prev=0;for(double s=0;s<=1;s+=0.1){float l=HornSize.lengthFor(s);if(l<prev)bad++;prev=l;}
 System.out.println("violations="+bad);
 int mx=0;for(long sd=0;sd<2000;sd++)mx=Math.max(mx,PartGenerators.build(PartKind.ANTLERS,1.0,sd).size());System.out.println("antler max nodes="+mx);
}}
