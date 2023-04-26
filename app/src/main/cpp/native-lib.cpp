#include <jni.h>
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <android/log.h>
#include <string>

using namespace cv;
using namespace std;

/*
extern "C" JNIEXPORT void JNICALL
Java_com_ksh_cvbasedsafetydriving_MainActivity_ConvertRGBtoGray(JNIEnv
* env,
jobject thiz, jlong
mat_addr_input,
jlong mat_addr_result
) {
Mat &matInput = *(Mat *)mat_addr_input;
Mat &matResult = *(Mat *)mat_addr_result;

cvtColor(matInput, matResult, COLOR_RGBA2GRAY);
}
*/

/*
 * extern "C"
JNIEXPORT void JNICALL
Java_com_ksh_cvbasedsafetydriving_MainActivity_ConvertRGBtoGray(JNIEnv
* env,
jobject thiz, jlong
mat_addr_input,
jlong mat_addr_result
) {
// TODO: implement ConvertRGBtoGray()
}
 */

float resize(Mat img_src, Mat &img_resize, int resize_width){


    float scale = resize_width / (float)img_src.cols ;

    if (img_src.cols > resize_width) {

        int new_height = cvRound(img_src.rows * scale);

        resize(img_src, img_resize, Size(resize_width, new_height));

    }

    else {

        img_resize = img_src;

    }

    return scale;

}

extern "C" JNIEXPORT jlong JNICALL
Java_com_ksh_cvbasedsafetydriving_CameraView_loadCascade(JNIEnv *env,

                                                                       jobject instance,

                                                                       jstring cascade_file_name) {


    const char *nativeFileNameString = env->GetStringUTFChars(cascade_file_name, 0);


    string baseDir("/storage/emulated/0/");

    baseDir.append(nativeFileNameString);

    const char *pathDir = baseDir.c_str();


    jlong ret = 0;

    ret = (jlong) new CascadeClassifier(pathDir);

    if (((CascadeClassifier *) ret)->empty()) {

        __android_log_print(ANDROID_LOG_DEBUG, "native-lib :: ",

                            "CascadeClassifier로 로딩 실패  %s", nativeFileNameString);

    }

    else

        __android_log_print(ANDROID_LOG_DEBUG, "native-lib :: ",

                            "CascadeClassifier로 로딩 성공 %s", nativeFileNameString);



    env->ReleaseStringUTFChars(cascade_file_name, nativeFileNameString);


    return ret;


}



extern "C" JNIEXPORT int JNICALL
Java_com_ksh_cvbasedsafetydriving_CameraView_detect(JNIEnv *env, jobject instance,

        jlong cascade_classifier_face, jlong cascade_classifier_body,

        jlong mat_addr_input,

jlong mat_addr_result) {


Mat &img_input = *(Mat *) mat_addr_input;

Mat &img_result = *(Mat *) mat_addr_result;


img_result = img_input.clone();


std::vector<Rect> faces;
std::vector<Rect> bodies;

Mat img_gray;


cvtColor(img_input, img_gray, COLOR_BGR2GRAY);

equalizeHist(img_gray, img_gray);


Mat img_resize;

float resizeRatio = resize(img_gray, img_resize, 640);
//resize(img_gray, img_resize, img_resize.size(), 0.5, 0.0, INTER_LINEAR);


//-- Detect faces

//((CascadeClassifier *) cascade_classifier_face)->detectMultiScale( img_resize, faces, 1.1, 2, 0|CASCADE_SCALE_IMAGE, Size(30, 30) );
//((CascadeClassifier *) cascade_classifier_face)->detectMultiScale( img_resize, faces, 1.1, 3, 0|CASCADE_SCALE_IMAGE, Size(10, 10) );



//__android_log_print(ANDROID_LOG_DEBUG, (char *) "native-lib :: ",
//(char *) "face %d found ", faces.size());

int min = 1000;
/*
for (int i = 0; i < faces.size(); i++) {
    int temp = ((int)(8414.7*pow(faces[faces.size() - 1].area(), -0.468)));
    if (min > temp) {
        min = temp;
    }
double real_facesize_x = faces[i].x / resizeRatio;

double real_facesize_y = faces[i].y / resizeRatio;

double real_facesize_width = faces[i].width / resizeRatio;

double real_facesize_height = faces[i].height / resizeRatio;


Point center( real_facesize_x + real_facesize_width / 2, real_facesize_y + real_facesize_height/2);

ellipse(img_result, center, Size( real_facesize_width / 2, real_facesize_height / 2), 0, 0, 360, Scalar(255, 0, 0), 5, 8, 0);

// Distance
//putText(img_result, to_string((int)(8414.7*pow(faces[faces.size() - 1].area(), -0.468))), center, 2, 2, Scalar(0, 0, 0), 3);

//Rect face_area(real_facesize_x, real_facesize_y, real_facesize_width,real_facesize_height);

//Mat faceROI = img_gray( face_area );
}
*/
((CascadeClassifier *) cascade_classifier_body)->detectMultiScale( img_resize, bodies, 1.1, 2, 0 | CASCADE_SCALE_IMAGE, Size(10, 10));

for (int i = 0; i < bodies.size(); i++) {
    int temp = ((int) (4 * 8414.7 * pow(bodies[bodies.size() - 1].area(), -0.468)));
    if (min > temp) {
    min = temp;
    }

double real_facesize_x = bodies[i].x / resizeRatio;

double real_facesize_y = bodies[i].y / resizeRatio;

double real_facesize_width = bodies[i].width / resizeRatio;

double real_facesize_height = bodies[i].height / resizeRatio;


Point center( real_facesize_x + real_facesize_width / 2, real_facesize_y + real_facesize_height/2);

ellipse(img_result, center, Size( real_facesize_width / 2, real_facesize_height / 2), 0, 0, 360, Scalar(255, 0, 0), 5, 8, 0);
}

return min;

/*
std::vector<Rect> eyes;

//-- In each face, detect eyes

((CascadeClassifier *) cascade_classifier_eye)->detectMultiScale( faceROI, eyes, 1.1, 2, 0 |CASCADE_SCALE_IMAGE, Size(30, 30) );


for ( size_t j = 0; j < eyes.size(); j++ )

{

Point eye_center( real_facesize_x + eyes[j].x + eyes[j].width/2, real_facesize_y + eyes[j].y + eyes[j].height/2 );

int radius = cvRound( (eyes[j].width + eyes[j].height)*0.25 );

circle( img_result, eye_center, radius, Scalar( 255, 0, 0 ), 30, 8, 0 );

}
*/
}

//}