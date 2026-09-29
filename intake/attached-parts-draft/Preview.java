import com.example.horsegenetics.common.parts.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

public class Preview {
    // rotation matrices, Minecraft order: Rz * Ry * Rx
    static double[][] mul(double[][] a, double[][] b){double[][] c=new double[3][3];for(int i=0;i<3;i++)for(int j=0;j<3;j++)for(int k=0;k<3;k++)c[i][j]+=a[i][k]*b[k][j];return c;}
    static double[][] rx(double t){double c=Math.cos(t),s=Math.sin(t);return new double[][]{{1,0,0},{0,c,-s},{0,s,c}};}
    static double[][] ry(double t){double c=Math.cos(t),s=Math.sin(t);return new double[][]{{c,0,s},{0,1,0},{-s,0,c}};}
    static double[][] rz(double t){double c=Math.cos(t),s=Math.sin(t);return new double[][]{{c,-s,0},{s,c,0},{0,0,1}};}
    static double[] app(double[][] m,double[] v){return new double[]{m[0][0]*v[0]+m[0][1]*v[1]+m[0][2]*v[2],m[1][0]*v[0]+m[1][1]*v[1]+m[1][2]*v[2],m[2][0]*v[0]+m[2][1]*v[1]+m[2][2]*v[2]};}

    record Face(double[][] p, Color c, double depth){}

    static void box(List<Face> out,double[] o,double[][] R,double gx,double len,Color base,int view){
        double h=gx/2;
        double[][] v=new double[8][];
        int n=0;
        for(int xi=0;xi<2;xi++)for(int yi=0;yi<2;yi++)for(int zi=0;zi<2;zi++){
            double[] l={xi==0?-h:h, yi==0?-len:0, zi==0?-h:h};
            double[] w=app(R,l); v[n++]=new double[]{o[0]+w[0],o[1]+w[1],o[2]+w[2]};
        }
        int[][] faces={{0,1,3,2},{4,5,7,6},{0,1,5,4},{2,3,7,6},{0,2,6,4},{1,3,7,5}};
        for(int[] f:faces){
            double[][] p=new double[4][]; double d=0;
            for(int i=0;i<4;i++){p[i]=v[f[i]]; d+=depth(p[i],view);} 
            // simple lambert from a fixed light in view space
            double[] a=sub(p[1],p[0]),b=sub(p[3],p[0]);double[] nrm=cross(a,b);double l=Math.sqrt(nrm[0]*nrm[0]+nrm[1]*nrm[1]+nrm[2]*nrm[2])+1e-9;
            double lit=0.55+0.45*Math.abs((nrm[0]*-0.3+nrm[1]*-0.6+nrm[2]*-0.5)/l);
            out.add(new Face(p,new Color(clamp(base.getRed()*lit),clamp(base.getGreen()*lit),clamp(base.getBlue()*lit),base.getAlpha()),d/4));
        }
    }
    static int clamp(double x){return (int)Math.max(0,Math.min(255,x));}
    static double[] sub(double[] a,double[] b){return new double[]{a[0]-b[0],a[1]-b[1],a[2]-b[2]};}
    static double[] cross(double[] a,double[] b){return new double[]{a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]};}
    // views: 0 side (screen x = z, front left; depth = x), 1 front (screen x = x; depth = z), 2 top (screen x = x, screen y = z; depth = -y)
    static double depth(double[] p,int view){return view==0?-p[0]:view==1?-p[2]:p[1];}
    static double[] proj(double[] p,int view){return view==0?new double[]{p[2],p[1]}:view==1?new double[]{p[0],p[1]}:new double[]{p[0],p[2]};}

    static void nodes(List<Face> out,List<PartNode> ns,double[] anchor,Color[] tex,int view){
        double[][][] R=new double[ns.size()][][];double[][] pos=new double[ns.size()][];
        for(int i=0;i<ns.size();i++){
            PartNode n=ns.get(i);
            double[][] loc=mul(rz(n.rz()),mul(ry(n.ry()),rx(n.rx())));
            if(n.parent()<0){R[i]=loc;pos[i]=new double[]{anchor[0]+n.ox(),anchor[1]+n.oy(),anchor[2]+n.oz()};}
            else{
                PartNode p=ns.get(n.parent());
                double[] along=app(R[n.parent()],new double[]{0,-p.len()*n.t(),0});
                double[] off=app(R[n.parent()],new double[]{n.ox(),n.oy(),n.oz()});
                pos[i]=new double[]{pos[n.parent()][0]+along[0]+off[0],pos[n.parent()][1]+along[1]+off[1],pos[n.parent()][2]+along[2]+off[2]};
                R[i]=mul(R[n.parent()],loc);
            }
            box(out,pos[i],R[i],n.girth(),n.len(),tex[n.tex()],view);
        }
    }
    static void hbox(List<Face> out,double x0,double y0,double z0,double x1,double y1,double z1,Color c,int view){
        // axis-aligned reference box: emit as identity-rotated box centred in x,z
        double[][] I=rx(0);
        // not centred: emulate with two calls is overkill - draw as one box of girth via origin shift
        double gx=x1-x0,gz=z1-z0; // ignore non-square: approximate with wider girth in the viewed axis
        double[] o={(x0+x1)/2,y1,(z0+z1)/2};
        // build faces manually
        double[][] v=new double[8][];int n=0;
        for(int xi=0;xi<2;xi++)for(int yi=0;yi<2;yi++)for(int zi=0;zi<2;zi++)v[n++]=new double[]{xi==0?x0:x1,yi==0?y0:y1,zi==0?z0:z1};
        int[][] faces={{0,1,3,2},{4,5,7,6},{0,1,5,4},{2,3,7,6},{0,2,6,4},{1,3,7,5}};
        for(int[] f:faces){double[][] p=new double[4][];double d=0;for(int i=0;i<4;i++){p[i]=v[f[i]];d+=depth(p[i],view);}out.add(new Face(p,c,d/4+1000));}
    }

    static void draw(Graphics2D g,List<Face> faces,int view,double ox,double oy,double sc){
        faces.sort(Comparator.comparingDouble(Face::depth).reversed());
        for(Face f:faces){
            Polygon poly=new Polygon();
            for(double[] p:f.p()){double[] q=proj(p,view);poly.addPoint((int)(ox+q[0]*sc),(int)(oy+q[1]*sc));}
            g.setColor(f.c());g.fillPolygon(poly);
            g.setColor(new Color(0,0,0,70));g.drawPolygon(poly);
        }
    }

    public static void main(String[] a) throws Exception{
        Color[] tex=new Color[16];Arrays.fill(tex,Color.GRAY);
        tex[PartSheet.HORN]=new Color(236,226,196);tex[PartSheet.HORN_TIP]=new Color(250,244,226);
        tex[PartSheet.BONE]=new Color(196,178,140);tex[PartSheet.BONE_TIP]=new Color(228,214,180);
        tex[PartSheet.CRYSTAL]=new Color(120,200,255,150);tex[PartSheet.CRYSTAL_CORE]=new Color(190,235,255,190);
        Color horse=new Color(120,84,60);
        String[] names={"horn NUB","horn SHORT","horn STANDARD","horn LONG","horn NARWHAL"};
        int W=1500,H=1150;
        BufferedImage img=new BufferedImage(W,H,BufferedImage.TYPE_INT_ARGB);Graphics2D g=img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(245,245,240));g.fillRect(0,0,W,H);
        g.setColor(Color.BLACK);g.setFont(new Font("SansSerif",Font.PLAIN,14));
        // Row 1: five horn sizes, side view on the head (head-local, before head_parts tilt)
        HornSize[] hs=HornSize.values();
        for(int i=0;i<hs.length;i++){
            List<Face> f=new ArrayList<>();
            hbox(f,-3,-11,-2,3,-6,5,horse,0);hbox(f,-2,-11,-7,2,-6,-2,horse,0);
            nodes(f,HornGenerator.generate(hs[i]),new double[]{0,-11,-0.5},tex,0);
            draw(g,f,0,120+i*290,260,9);g.setColor(Color.BLACK);g.drawString(names[i]+" ("+hs[i].length+")",50+i*290,300);
        }
        // Row 2: antlers front, side, and three seeds/sizes front view
        double[] sizes={0.0,0.5,1.0};long[] seeds={11,22,33};
        for(int i=0;i<3;i++){
            List<Face> f=new ArrayList<>();
            hbox(f,-3,-11,-2,3,-6,5,horse,1);
            hbox(f,-3.6,-13,4,-1.6,-10,5,horse,1);hbox(f,1.6,-13,4,3.6,-10,5,horse,1);
            List<PartNode> ns=PartGenerators.build(PartKind.ANTLERS,sizes[i],seeds[i]);
            nodes(f,ns,new double[]{0,-11,3},tex,1);
            draw(g,f,1,150+i*300,600,9);g.setColor(Color.BLACK);g.drawString("antlers front size="+sizes[i]+" nodes="+ns.size(),50+i*300,640);
        }
        {
            List<Face> f=new ArrayList<>();hbox(f,-3,-11,-2,3,-6,5,horse,0);hbox(f,-2,-11,-7,2,-6,-2,horse,0);
            nodes(f,PartGenerators.build(PartKind.ANTLERS,1.0,33),new double[]{0,-11,3},tex,0);
            draw(g,f,0,1140,600,9);g.setColor(Color.BLACK);g.drawString("antlers side size=1",1050,640);
        }
        // Row 3: crystals, side and top, sizes 0.2 / 0.6 / 1.0 on the body top (y=-8)
        double[] cs={0.2,0.6,1.0};
        for(int i=0;i<3;i++){
            List<Face> f=new ArrayList<>();hbox(f,-5,-8,-17,5,2,5,horse,0);
            List<PartNode> ns=PartGenerators.build(PartKind.CRYSTALS,cs[i],7+i);
            nodes(f,ns,new double[]{0,-8,-6},tex,0);
            draw(g,f,0,150+i*330,900,9);g.setColor(Color.BLACK);g.drawString("crystals side size="+cs[i]+" nodes="+ns.size(),50+i*330,1010);
        }
        {
            List<Face> f=new ArrayList<>();hbox(f,-5,-8,-17,5,2,5,horse,2);
            nodes(f,PartGenerators.build(PartKind.CRYSTALS,1.0,9),new double[]{0,-8,-6},tex,2);
            draw(g,f,2,1330,960,9);g.setColor(Color.BLACK);g.drawString("crystals top",1250,1130);
        }
        ImageIO.write(img,"png",new java.io.File("preview.png"));
        // node counts summary
        for(HornSize h:HornSize.values())System.out.println(h+" nodes="+HornGenerator.generate(h).size());
        for(double s:new double[]{0,0.5,1}){int mx=0;for(long sd=0;sd<200;sd++)mx=Math.max(mx,PartGenerators.build(PartKind.ANTLERS,s,sd).size());System.out.println("antlers size "+s+" max nodes over 200 seeds="+mx);}
        for(double s:new double[]{0,1})System.out.println("crystals size "+s+" nodes="+PartGenerators.build(PartKind.CRYSTALS,s,1).size());
    }
}
