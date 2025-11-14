# MIDI-Mapper

Android app that works as a MIDI host and bridge. It allows intercepting incoming CC messages from a source device and sending custom SysEx messages instead.

## Features

*   **MIDI Host & Bridge:** Connect to MIDI devices and route messages between them.
*   **CC to SysEx Conversion:** Intercept incoming MIDI CC messages and send custom SysEx messages instead.
*   **MIDI Learn:** Easily map a hardware control (knob/slider) to the app.
*   **Customizable SysEx:** Define your own SysEx message format, using `%V` as a placeholder for the value.
*   **On-Screen Control:** Use a virtual knob to send messages without a hardware controller.

## Status

This project is currently a work in progress. However, you can download a debug version of the app from the [GitHub Releases page](https://github.com/Barsik-Barbosik/MIDI-Mapper/releases).

Supported Android version: 6.0 (Marshmallow) and above.

## How to Use

1.  Download the latest debug APK from the [GitHub Releases page](https://github.com/Barsik-Barbosik/MIDI-Mapper/releases).
2.  Install the APK on your Android device.
3.  Connect your source and target MIDI devices to your Android device (e.g., using a USB OTG adapter).
4.  Open the MIDI-Mapper app.
5.  Select the source and target devices from the dropdown menus.
6.  Click "Connect".
7.  Click "Learn CC" and move a knob/slider on your source device to map it.
8.  (Optional) Customize the SysEx message. `%V` will be replaced with the knob's value (0-127).
9.  Move the mapped control on your source device or use the on-screen knob to send SysEx messages to your target device.

## Building from Source

1.  Clone the repository: `git clone https://github.com/Barsik-Barbosik/MIDI-Mapper.git`
2.  Open the project in Android Studio.
3.  Let Gradle sync the project.
4.  Build the project using `Build > Make Project` or run it on an emulator or a connected device.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
