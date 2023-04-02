package com.waveshare.epaperesp32loader.image_processing;

/**
 * Created by YiWan on 8/24/2018.
 */

/**
 * <h1>e-Paper display characteristics</h1>
 * The class is intended to store width, height and index of palette.
 *
 * @author  Waveshare team
 * @version 1.0
 * @since   8/14/2018
 */

public class EPaperDisplay
{
    public int    width;
    public int    height;
    public int    index;
    public String title;

    /**
     * Stores display's characteristics.
     * @param width of display in pixels
     * @param height of display on pixels
     * @param index of palette
     * @param title of display
     */
    public EPaperDisplay(int width, int height, int index, String title)
    {
        this.width  = width;
        this.height = height;
        this.index  = index;
        this.title  = title;
    }

    // Index of selected display
    //---------------------------------------------------------
    public static int epdInd = -1;

    // Array of display characteristics
    //---------------------------------------------------------
    private static EPaperDisplay[] array = null;

    public static EPaperDisplay[] getDisplays()
    {
        if (array == null)
        {
            array = new EPaperDisplay[]
            {
                new EPaperDisplay(200,200,0, "1.54 inch e-Paper"),      // 0
                new EPaperDisplay(200,200,3, "1.54 inch e-Paper (B)"),  // 1
                new EPaperDisplay(152,152,5, "1.54 inch e-Paper (C)"),  // 2
                new EPaperDisplay(122,250,0, "2.13 inch e-Paper"),      // 3
                new EPaperDisplay(104,212,1, "2.13 inch e-Paper (B)"),  // 4
                new EPaperDisplay(104,212,5, "2.13 inch e-Paper (C)"),  // 5
                new EPaperDisplay(104,212,0, "2.13 inch e-Paper (D)"),  // 6
                new EPaperDisplay(176,264,0, "2.7 inch e-Paper"),       // 7
                new EPaperDisplay(176,264,1, "2.7 inch e-Paper (B)"),   // 8
                new EPaperDisplay(128,296,0, "2.9 inch e-Paper"),       // 9
                new EPaperDisplay(128,296,1, "2.9 inch e-Paper (B)"),   // 10
                new EPaperDisplay(128,296,5, "2.9 inch e-Paper (C)"),   // 11
                new EPaperDisplay(128,296,0, "2.9 inch e-Paper (D)"),   // 12
                new EPaperDisplay(400,300,0, "4.2 inch e-Paper"),       // 13
                new EPaperDisplay(400,300,1, "4.2 inch e-Paper (B)"),   // 14
                new EPaperDisplay(400,300,5, "4.2 inch e-Paper (C)"),   // 15
                new EPaperDisplay(600,448,0, "5.83 inch e-Paper"),      // 16
                new EPaperDisplay(600,448,1, "5.83 inch e-Paper (B)"),  // 17
                new EPaperDisplay(600,448,5, "5.83 inch e-Paper (C)"),  // 18
                new EPaperDisplay(640,384,0, "7.5 inch e-Paper"),       // 19
                new EPaperDisplay(640,384,1, "7.5 inch e-Paper (B)"),   // 20
                new EPaperDisplay(640,384,5, "7.5 inch e-Paper (C)"),   // 21
                new EPaperDisplay(800,480,0, "7.5 inch e-Paper V2"),        // 22
                new EPaperDisplay(800,480,1, "7.5 inch e-Paper (B) V2"),    // 23
                new EPaperDisplay(880,528,1, "7.5 inch HD e-Paper (B)"),    // 24
                new EPaperDisplay(600,448,7, "5.65 inch e-Paper (F)"),      // 25
                new EPaperDisplay(880,528,0, "7.5 inch HD e-Paper"),        // 26
                new EPaperDisplay(280,480,0, "3.7 inch e-Paper"),           // 27
                new EPaperDisplay(152,296,0, "2.66 inch e-Paper"),          // 28
                new EPaperDisplay(648,480,1, "5.83 inch e-Paper (B) V2"),   // 29
                new EPaperDisplay(128,296,1, "2.9 inch e-Paper (B) V3"),    // 30
                new EPaperDisplay(200,200,1, "1.54 inch e-Paper (B) V2"),   // 31
                new EPaperDisplay(104,214,1, "2.13 inch e-Paper (B) V3"),   // 32
                new EPaperDisplay(128,296,0, "2.9 inch e-Paper V2"),        // 33
                new EPaperDisplay(400,300,1, "4.2 inch e-Paper (B) V2"),    // 34
                new EPaperDisplay(152,296,1, "2.66 inch e-Paper (B)"),      // 35
                new EPaperDisplay(648,480,0, "5.83 inch e-Paper V2"),       // 36
                new EPaperDisplay(640,400,7, "4.01 inch e-Paper (F)"),      // 37
                new EPaperDisplay(176,264,1, "2.7 inch e-Paper (B) V2"),    // 38
                new EPaperDisplay(122,250,0, "2.13 inch e-Paper V3"),       // 39
                new EPaperDisplay(122,250,1, "2.13 inch e-Paper (B) V4"),   // 40
                new EPaperDisplay(240,360,0, "3.52 inch e-Paper"),          // 41
                new EPaperDisplay(176,264,0, "2.7 inch e-Paper V2")         // 42
            };
        }
        return array;
    }
}