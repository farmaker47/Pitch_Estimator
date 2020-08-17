# Pitch_Estimator
Music Pitch detection using Tensorflow SPICE model.

Pitch is an attribute of musical tones (along with duration, intensity and timbre) that allows you to describe a note as “high” or “low”. Pitch is quantified by frequency, measured in Hertz (Hz), where one Hz corresponds to one cycle per second. The higher the frequency, the higher the note.

Pitch detection is an interesting challenge. Historically, for a machine to understand pitch, it would need to rely on complex hand-crafted signal-processing algorithms to measure the frequency of a note, in particular to separate the relevant frequency from background noise and backing instruments. Today, we can do that with machine learning, more specifically with the SPICE model (SPICE: Self-Supervised Pitch Estimation).

SPICE is a pretrained model that can recognize the fundamental pitch from mixed audio recordings (including noise and backing instruments).

You can follow along with this [Colab notebook](https://colab.sandbox.google.com/github/tensorflow/hub/blob/master/examples/colab/spice.ipynb).

[Demo](https://youtu.be/v1d3o4r40PQ) of the project

**Screenshot**

<img src="shot.jpg" width="256" height="540">
