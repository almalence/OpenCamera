/*
The contents of this file are subject to the Mozilla Public License
Version 1.1 (the "License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
License for the specific language governing rights and limitations
under the License.

The Original Code is collection of files collectively known as Open Camera.

The Initial Developer of the Original Code is Almalence Inc.
Portions created by Initial Developer are Copyright (C) 2013
by Almalence Inc. All Rights Reserved.
*/

#ifndef __FACEDETECTOR_H__
#define __FACEDETECTOR_H__

int  FaceDetector_initialize(void **instance, int w, int h, int maxFaces);
void FaceDetector_destroy(void * instance);
int  FaceDetector_detect(void * instance, unsigned char *bwbuffer);
void FaceDetector_get_face(void *instance, float *confid, float *midx, float *midy, float *eyedist);


#endif // __FACEDETECTOR_H__
