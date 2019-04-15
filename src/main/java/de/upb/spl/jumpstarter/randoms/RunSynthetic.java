package de.upb.spl.jumpstarter.randoms;


import de.upb.spl.guo11.Guo11;
import de.upb.spl.hasco.HASCOSPLReasoner;
import de.upb.spl.henard.Henard;
import de.upb.spl.hierons.Hierons;
import de.upb.spl.ibea.BasicIbea;
import de.upb.spl.jumpstarter.Reasoner;
import de.upb.spl.reasoner.SPLReasoner;
import de.upb.spl.reasoner.SampleReasoner;
import de.upb.spl.sayyad.Sayyad;
import de.upb.spl.util.FileUtil;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public class RunSynthetic extends SyntheticEnv {

//    @Reasoner
    public List<SPLReasoner> samples() {
        return IntStream.range(0, 10)
                .mapToObj(i -> String.format("/samples/random-sat-%d.json", i))
                .map(file ->  new SampleReasoner(FileUtil.getPathOfResource(SPL_NAME) + file))
                .collect(Collectors.toList());
    }

    @Reasoner(order = 1)
    public SPLReasoner guo() {
        Guo11 guo11 = new Guo11();
        return guo11;
    }

//     @Reasoner(order = 1, enabled = true)
    public SPLReasoner ibea() {
        BasicIbea basicIbea = new BasicIbea();
        return basicIbea;
    }

    @Reasoner(order = 2)
    public SPLReasoner sayyad() {
        Sayyad sayyad = new Sayyad();
        return sayyad;
    }



    @Reasoner(order = 3)
    public SPLReasoner henard()  {
        Henard henard = new Henard();
        return henard;
    }



    @Reasoner(order = 4)
    public SPLReasoner hierons() {
        Hierons hierons = new Hierons();
        return hierons;
    }


    @Reasoner(order = 10)
    public SPLReasoner hasco() {
        HASCOSPLReasoner hasco = new HASCOSPLReasoner();
        return hasco;
    }


    public static void main(String... args) {
        new RunSynthetic().setup();
    }
}
