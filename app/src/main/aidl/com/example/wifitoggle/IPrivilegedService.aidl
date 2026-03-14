package com.example.wifitoggle;

import android.os.Bundle;

interface IPrivilegedService {
    Bundle runCommand(String command);
}
