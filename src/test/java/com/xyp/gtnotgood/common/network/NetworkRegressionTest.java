package com.xyp.gtnotgood.common.network;

import org.junit.Test;

/** Makes the existing headless network regression suite discoverable by Gradle and GitHub CI. */
public class NetworkRegressionTest {

    @Test
    public void networkRegression() throws Exception {
        NetworkRegressionChecks.main(new String[0]);
    }
}
