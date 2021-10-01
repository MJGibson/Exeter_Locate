package com.riba2reality.exeterlocatecore;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

//@RunWith(MockitoJUnitRunner.class)
public class TrackerScannerUnitTest {


//    @Mock
//    TrackerScanner testSubjectClass;
//
//
//    //==============================================================================================
//    @Test
//    public void test_onCreate(){
//
//        ////testSubjectClass.onCreate();
//        testSubjectClass = new TrackerScanner();
//
//
//        assertNotNull(testSubjectClass);
//        assertNotNull(testSubjectClass.broadcaster);
//
//
//        //assertEquals(4, 2 + 2);
//
//    }
//    //==============================================================================================





    //==============================================================================================
    @Test
    public void getPoisson_tests() {

        double lambda = 60.0;

        double meanValue = 0.0;

        int n = 1000000;
        for(int i = 0; i < n; ++i){

            meanValue += TrackerScanner.getPoisson(lambda);

        }

        meanValue /= n;

        assertEquals(lambda, meanValue, 0.1);


        System.out.println( "meanValue: "+Double.toString(meanValue)
                +", expected: "+Double.toString(lambda) +
                ",from N= "+Integer.toString(n)+" trials."
        );


    }// end of getPoisson_tests
    //==============================================================================================



}// end of TrackerScannerUnitTest class
