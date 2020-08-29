# Pitch_Estimator
Music Pitch detection using Tensorflow SPICE model.

Pitch is a perceptual property of sounds that allows their ordering on a frequency-related scale, or more commonly, pitch is the quality that makes it possible to judge sounds as “higher” and “lower” in the sense associated with musical melodies. Pitch is a major auditory attribute of musical tones, along with [duration](https://en.wikipedia.org/wiki/Duration_(music)), [loudness](https://en.wikipedia.org/wiki/Loudness), and [timbre](https://en.wikipedia.org/wiki/Timbre), is quantified by frequency and measured in Hertz (Hz), where one Hz corresponds to one cycle per second.

Pitch detection is an interesting challenge. Historically, the study of pitch and pitch perception has been a central problem in psychoacoustics, and has been instrumental in forming and testing theories of sound representation, [signal-processing algorithms](https://en.wikipedia.org/wiki/Pitch_detection_algorithm), and perception in the auditory system. A lot of [techniques](https://www.cs.uregina.ca/Research/Techreports/2003-06.pdf) have been used for this purpose. Efforts have also been made to separate the relevant frequency from background noise and backing instruments.

Today, we can do that with machine learning, more specifically with the SPICE model ([SPICE: Self-Supervised Pitch Estimation](https://ai.googleblog.com/2019/11/spice-self-supervised-pitch-estimation.html)). This is a pretrained model that can recognize the fundamental pitch from mixed audio recordings (including noise and backing instruments).The model is available to use through [TensorFlow Hub](https://tfhub.dev/), on the web with [TensorFlow.js](https://tfhub.dev/google/tfjs-model/spice/1/default/1) and on mobile devices with [TensorFlow Lite](https://tfhub.dev/google/lite-model/spice/1).

You can follow along with this [Colab notebook](https://colab.sandbox.google.com/github/tensorflow/hub/blob/master/examples/colab/spice.ipynb).

**Note:** Application saves audio file in .wav format inside phone's internal storage memory in Pitch Detector folder so anyone can compare results with colab notebook output. For this purpose and for this demo version of the application, writing internal storage permission is mandatory for app to work! 

[Demo](https://youtu.be/v1d3o4r40PQ) of the project

**Screenshot**

<img src="shot.jpg" width="256" height="540">
