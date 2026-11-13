#![deny(unsafe_op_in_unsafe_fn)]
use core::ffi::c_void;
#[no_mangle]
pub extern "C" fn pocketforge_abi_version() -> u32 { 1 }
#[no_mangle]
pub extern "C" fn pocketforge_health_check() -> i32 { 0 }
#[no_mangle]
pub extern "system" fn Java_com_pocketforge_app_MainActivity_nativeAbiVersion(_env:*mut c_void,_obj:*mut c_void)->i32 { 1 }
#[no_mangle]
pub extern "system" fn Java_com_pocketforge_app_MainActivity_nativeHealthCheck(_env:*mut c_void,_obj:*mut c_void)->i32 { 0 }
