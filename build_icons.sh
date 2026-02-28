#!/bin/bash
cd /tmp/sukun_app_icon

# Correct image path
cp /Users/muhammadramdhan/.gemini/antigravity/brain/26fe2b4a-b4cd-45d5-a88d-bed73b1ec5e1/media__1772185775933.png source.png

# Create a master 1024x1024 icon
sips -z 1024 1024 source.png --out ic_launcher.png

# Android mipmap directories
MIPMAP_DIR="/Users/muhammadramdhan/StudioProjects/Sukun/app/src/main/res"

# Generate sizes (launcher)
sips -z 48 48 ic_launcher.png --out $MIPMAP_DIR/mipmap-mdpi/ic_launcher.png
sips -z 72 72 ic_launcher.png --out $MIPMAP_DIR/mipmap-hdpi/ic_launcher.png
sips -z 96 96 ic_launcher.png --out $MIPMAP_DIR/mipmap-xhdpi/ic_launcher.png
sips -z 144 144 ic_launcher.png --out $MIPMAP_DIR/mipmap-xxhdpi/ic_launcher.png
sips -z 192 192 ic_launcher.png --out $MIPMAP_DIR/mipmap-xxxhdpi/ic_launcher.png

# Generate sizes (launcher_round - Android will handle rounding if needed but we provide the same as round for older devices)
sips -z 48 48 ic_launcher.png --out $MIPMAP_DIR/mipmap-mdpi/ic_launcher_round.png
sips -z 72 72 ic_launcher.png --out $MIPMAP_DIR/mipmap-hdpi/ic_launcher_round.png
sips -z 96 96 ic_launcher.png --out $MIPMAP_DIR/mipmap-xhdpi/ic_launcher_round.png
sips -z 144 144 ic_launcher.png --out $MIPMAP_DIR/mipmap-xxhdpi/ic_launcher_round.png
sips -z 192 192 ic_launcher.png --out $MIPMAP_DIR/mipmap-xxxhdpi/ic_launcher_round.png

echo "Icons generated successfully"
