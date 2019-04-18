package de.upb.spl.jumpstarter.randoms;


import de.upb.spl.guo11.Guo11;
import de.upb.spl.hasco.HASCOSPLReasoner;
import de.upb.spl.henard.Henard;
import de.upb.spl.hierons.Hierons;
import de.upb.spl.ibea.BasicIbea;
import de.upb.spl.jumpstarter.Env;
import de.upb.spl.jumpstarter.GUI;
import de.upb.spl.jumpstarter.Reasoner;
import de.upb.spl.reasoner.SPLReasoner;
import de.upb.spl.reasoner.SampleReasoner;
import de.upb.spl.sayyad.Sayyad;
import de.upb.spl.util.FileUtil;

@Env(randomize =true)
@GUI(enabled = false)
public class RunSynthetic extends SyntheticEnv {

//    @Reasoner(order = -1, times = 10)
    public SPLReasoner samples(int i) {
        return new SampleReasoner(
                FileUtil.getPathOfResource(SPL_NAME) + String.format("/samples/random-sat-%d.json", i));
    }

//    @Reasoner(order = 1, times = 10)
    public SPLReasoner guo(int i) {
        Guo11 guo11 = new Guo11("-" + i, true);
        return guo11;
    }

//     @Reasoner(order = 1, enabled = true)
    public SPLReasoner ibea() {
        BasicIbea basicIbea = new BasicIbea();
        return basicIbea;
    }

//    @Reasoner(order = 2, times = 10)
    public SPLReasoner sayyad(int i) {
        Sayyad sayyad = new Sayyad("-" + i, true);
        return sayyad;
    }



//    @Reasoner(order = 3, times = 10)
    public SPLReasoner henard(int i)  {
        Henard henard = new Henard("-" + i, true);
        return henard;
    }



//    @Reasoner(order = 4, times = 10)
    public SPLReasoner hierons(int i) {
        Hierons hierons = new Hierons("-" + i, true);
        return hierons;
    }


    @Reasoner(order = 10, times = 1)
    public SPLReasoner hasco(int i) {
        HASCOSPLReasoner hasco = new HASCOSPLReasoner("-" + i, true);
        return hasco;
    }

    public static void main(String... args) {
        new RunSynthetic().setup();
    }
}
