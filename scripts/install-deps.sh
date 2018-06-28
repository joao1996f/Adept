#!/usr/bin/env bash

# Get OS name
# Detecting "Darwin" returns "macOS".
# Detecting "Linux" returns the name of the specific distro.
#
# output: OS name
# return: Detection success
get_os() {
    # shellcheck disable=2155
    declare -r OS_NAME="$(uname -s)"
    local os=""
    local retval=1

    if [ "$OS_NAME" = "Darwin" ]; then
        os="macOS"
        retval=0
    elif [ "$OS_NAME" = "Linux" ] && [ -e "/etc/os-release" ]; then
        # shellcheck disable=SC1090
        source <(grep NAME</etc/os-release)
        os=${NAME// /_}
        unset NAME
        unset PRETTY_NAME
        retval=0
    fi

    printf "%s" "$os"
    return $retval
}

# Install all dependencies for the selected OS. If the OS name provided isn't
# supported, prompt the user to open a PR and add the correct installation
# command.
# $1 => Name of OS
#
# return: 0 for success or 1 for failure
install_dependencies() {
    # Store os_name
    local os_name=$1
    local retval=1

    echo "You may be asked for sudo priviledges"

    if [ "$os_name" == "macOS" ]; then
        # Run brew command here

        # Check if Homebrew is installed before running command
        brew_installed=$(which brew > /dev/null; echo $?)
        if ! [ "$brew_installed" -eq 0 ]; then
            echo "Homebrew isn't installed in your system"
            echo "Please go to https://brew.sh/, follow the instructions and execute"
            echo "this script again."

            return $retval
        fi

        brew update
        brew install sbt verilator

        retval=$?
    elif [ "$os_name" == "Ubuntu" ] || [ "$os_name" == "Debian_GNU/Linux" ]; then
        # Run Ubuntu and Debian command here

        # Installing sbt, requires an additional ppa
        # Check if user already has it
        if [ ! -f /etc/apt/sources.list.d/sbt.list ]; then
            echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
            sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 642AC823
        fi

        sudo apt -qq update

        # In latest Debian (Stretch) verilator is version 3.9 and in Ubuntu
        # (18.04) its version 3.916
        sudo apt install -y default-jdk sbt

        sudo apt install -y make git autoconf g++ flex bison
        git clone http://git.veripool.org/git/verilator
        cd verilator
        git pull
        git checkout verilator_3_904
        autoconf
        ./configure
        make
        sudo make install
        cd ..

        retval=$?
    else
        echo "Unsupported OS :("
        echo "We accept pull requests though :)"
    fi

    return $retval
}

################################################################################
## Script Starts here!
################################################################################

# Get OS name and install all dependencies. Exit script if it fails
os_name=$(get_os) || exit
install_dependencies "${os_name}" || exit
