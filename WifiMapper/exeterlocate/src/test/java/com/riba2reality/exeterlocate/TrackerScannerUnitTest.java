package com.riba2reality.exeterlocate;

import com.riba2reality.wifimapper.TrackerScanner;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TrackerScannerUnitTest {

    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }


    @Test
    public void getPoisson_tests()
    {
        //assertEquals(4, 2 + 2);

        double lambda = 60.0;

        double meanValue = 0.0;

        int n = 10000;
        for(int i = 0; i < n; ++i){

            meanValue += TrackerScanner.getPoisson(lambda);

        }

        meanValue /= n;

        assertEquals(lambda, meanValue, 0.1);




    }



}// end of TrackerScannerUnitTest class
