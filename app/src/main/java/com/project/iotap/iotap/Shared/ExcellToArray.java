package com.project.iotap.iotap.Shared;

import android.util.Log;

import com.project.iotap.iotap.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Created by anton on 1/9/18.
 */

public class ExcellToArray {

    String upData = "36,21,-162,71,133,79,28,0,-105,-237,1076,170,17,-39,0,17,1137,-136,11,-23,44,123,1358,-239,-14,-7,130,-240,1363,-244,-51,26,296,835,250,-61,-75,27,141,30,-1372,200,-106,38,142,-492,-1633,183,-55,25,127,-271,-2981,453,64,-24,-150,178,-3122,-8,68,-51,-198,-732,-1166,137,72,-12,-187,48,-295,-138,36,9,-77,322,436,68,84,-19,-204,-581,1185,-222,20,-8,-110,2021,1679,213,-72,33,105,-2143,737,-219,-22,-25,70,1248,823,-8,-7,20,73,-271,87,-102,0,-6,16,67,243,1,-6,7,22,81,-11,-41";
    String downData = "105,3,119,-55,-1166,41,31,-10,31,118,-738,0,77,12,-1,291,-635,-81,97,13,-49,-392,-165,-34,106,-18,-38,286,-527,150,27,-12,-115,-221,884,-182,-14,39,-173,742,2014,84,-60,37,91,-1954,2052,-126,29,-12,23,758,1110,403,3,-56,104,-873,555,-274,-42,-21,42,1464,69,-85,-68,-11,32,-293,105,-115,-104,35,80,438,-229,159,-127,8,89,-623,-584,191,-30,-8,4,529,-1593,-76,-7,-1,-93,-161,-908,-34,0,-4,-98,20,-289,-93,5,8,-50,-129,53,-19,-19,8,-5,-177,22,24,9,-7,-4,35,66,22";
    String leftData = "47,-153,22,543,-423,-1975,42,-6,3,330,-125,-1279,59,33,19,-10,-89,-1124,92,38,36,26,-209,-1061,72,130,-25,758,-264,-436,14,213,-35,-399,171,1422,-31,95,-46,-886,453,1708,14,207,-38,-816,1112,3065,-79,-249,69,-430,125,1668,11,-67,12,464,-179,1521,10,-83,-20,-637,362,809,20,-64,4,8,133,833,-13,-95,12,172,405,-156,-50,-45,35,-411,-288,-45,-87,-117,35,95,-96,-1039,-79,-82,-2,1450,-685,-1857,-3,59,-45,-155,-257,-1597,-35,108,-8,113,-25,-329,-12,42,-7,-112,-163,-256,4,38,-15,-37,-81,58";
    String rightData = "76,-152,32,-742,416,3228,-9,-106,3,650,-177,-1144,-22,-58,-9,-14,-55,-1343,-16,17,-1,165,-133,-1137,0,32,11,4,-67,-881,20,51,2,105,-142,-848,30,59,-16,479,-202,-878,16,45,-6,147,220,-649,7,95,12,97,-25,-73,-27,148,0,-168,-97,555,-63,138,-31,-398,-233,1153,-13,-114,3,-482,199,1185,-4,-3,-46,52,170,807,-1,-96,22,90,-170,130,-3,-42,-1,-251,479,193,-6,-28,21,190,-142,-114,-3,-5,2,14,74,17,-1,-6,9,-96,-40,-95,-4,3,-3,189,-89,-70,-2,9,-14,-15,-1,-36";

    public int[][] getArray(int gesture) {
        int[][] array = new int[20][6];
        String currentgestureRawData = null;
        switch (gesture) {
            case 1:
                currentgestureRawData = upData;
                break;
            case 2:
                currentgestureRawData = downData;
                break;
            case 3:
                currentgestureRawData = leftData;
                break;
            case 4:
                currentgestureRawData = rightData;
                break;
        }
        String[] splitData = currentgestureRawData.split(",");

        int counter = 0;

        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[i].length; j++) {
                array[i][j] = Integer.valueOf(splitData[counter++]);
            }
        }
        return array;
    }
}