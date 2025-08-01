#!/bin/bash
set -e

# ASCII Art Banner
cat << 'EOF'
╔══════════════════════════════════════════════════════════════╗
║                                                              ║
║    🚀 OpenTelemetry Inspector Dev Container Setup 🚀        ║
║                                                              ║
║    🎯 Setting up your development environment...            ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝
EOF

echo ""
echo "🔄 Starting initialization process..."
echo ""

# Install Azure CLI
echo "☁️  Installing Azure CLI..."
curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash
echo "✅ Azure CLI installation complete!"
echo ""

# Install Python
echo "🐍 Installing Python..."
sudo apt-get update
sudo apt-get install -y python3 python3-pip

# add pyhton and pip to path
echo "🔗 Adding Python to system PATH..."
echo "export PATH=\$PATH:/usr/bin/python3" >> /etc/profile.d/python.sh
echo "export PATH=\$PATH:/usr/bin/pip3" >> /etc/profile.d/python.sh
echo "✅ Python installation complete!"

echo ""
echo "🎉 Setup complete! Your development environment is ready!"
echo ""
echo "📋 Summary of installed tools:"
echo "   ☁️  Azure CLI"
echo "   🐹 Go $GO_VERSION"
echo ""
echo " Happy coding! 🚀"
