#!/bin/bash

# Git Cleanup Script for Android Project
# This script helps maintain a clean git repository

echo "🧹 Starting Git Cleanup..."

# Remove untracked files and directories
echo "📁 Removing untracked files..."
git clean -fd

# Remove .gradle directory from tracking if it exists
if [ -d ".gradle" ]; then
    echo "🗑️  Removing .gradle from git tracking..."
    git rm -r --cached .gradle/ 2>/dev/null || true
fi

# Remove build directories from tracking
echo "🏗️  Removing build directories from tracking..."
git rm -r --cached app/build/ 2>/dev/null || true
git rm -r --cached build/ 2>/dev/null || true

# Remove IDE files from tracking
echo "💻 Removing IDE files from tracking..."
git rm -r --cached .idea/ 2>/dev/null || true
git rm --cached *.iml 2>/dev/null || true

# Show current status
echo "📊 Current git status:"
git status --porcelain

echo "✅ Git cleanup completed!"
echo ""
echo "💡 Tips:"
echo "   - Run this script regularly to keep your repo clean"
echo "   - Check .gitignore if new unwanted files appear"
echo "   - Use 'git status' to see what files are being tracked"
