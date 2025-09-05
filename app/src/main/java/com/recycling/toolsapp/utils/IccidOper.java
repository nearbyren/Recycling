package com.recycling.toolsapp.utils;


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

public class IccidOper {

    private static String TAG = "IccidOper";
    private static String AT_ICCID = "echo -e  \"AT+CCID\r\n\" > /dev/ttyUSB2";
    private static String AT_GREP = "logcat  -b radio -t 20 | grep CCID";
    //在自己内部定义自己的一个实例，只供内部调用
    private static final IccidOper instance = new IccidOper();

    public String GetIccid() {
        String iccid = "", ret = "";

        execSuCmd("chmod 777 /dev/ttyUSB*");
        execSuCmd(AT_ICCID);
        ret = execSuCmd(AT_GREP);

       System.out.println( "=========" + ret.length());

        if (ret != null && !"".equals(ret)) {
            int index = ret.lastIndexOf("CCID:");
            if (index < 0) {
                return null;
            }

            iccid = ret.substring(index + 6);

        }
        return iccid;
    }


    public String execSuCmd(String command) {

        StringBuffer output = new StringBuffer();
        Process process = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            //Log.e(TAG, "executer:" +command );
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();

            process.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = "";
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }
        } catch (Exception e) {
            //e.printStackTrace();
        } finally {
            try {
                if (os != null)
                    os.close();
                if (process != null)
                    process.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        String response = output.toString().trim();//.substring(2, output.length() - 1);
        return response;
    }

    //这里提供了一个供外部访问本class的静态方法，可以直接访问
    public static IccidOper getInstance() {
        return instance;
    }

}
