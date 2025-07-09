/*
 * RinBounce Background Shader
 * Blue to white gradient without circular animations
 */

#version 120

#ifdef GL_ES
precision lowp float;
#endif

uniform float iTime;
uniform vec2 iResolution;

// Simple hash function for noise
float hash(float n) {
    return fract(sin(n) * 43758.5453);
}

// 2D noise function
float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i.x + hash(i.y)), hash(i.x + 1.0 + hash(i.y)), u.x),
               mix(hash(i.x + hash(i.y + 1.0)), hash(i.x + 1.0 + hash(i.y + 1.0)), u.x), u.y);
}

// Simple gradient layer without circular effects
vec3 gradientLayer(vec2 uv, float speed, float intensity, vec3 color1, vec3 color2) {
    float t = iTime * speed;
    
    // Simple vertical gradient with subtle horizontal wave
    float gradient = uv.y + sin(uv.x * 3.0 + t) * 0.05;
    gradient = smoothstep(0.0, 1.0, gradient);
    
    return mix(color1, color2, gradient) * intensity;
}

// Subtle wave effect
float waves(vec2 uv, float time) {
    float wave1 = sin(uv.x * 6.0 + time * 1.0) * 0.05;
    float wave2 = sin(uv.x * 10.0 - time * 0.8) * 0.03;
    
    return (wave1 + wave2) * smoothstep(0.0, 0.2, uv.y) * smoothstep(1.0, 0.8, uv.y);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord / iResolution.xy;
    
    // Base gradient colors (blue to white)
    vec3 topColor = vec3(0.055, 0.482, 0.788);    // Blue
    vec3 bottomColor = vec3(0.737, 0.749, 0.761); // White
    
    // Create simple animated gradient
    vec3 color = gradientLayer(uv, 0.05, 1.0, topColor, bottomColor);
    
    // Add secondary gradient layer for depth
    vec3 secondaryTop = vec3(0.15, 0.35, 0.85);
    vec3 secondaryBottom = vec3(0.9, 0.95, 1.0);
    color += gradientLayer(uv, 0.03, 0.2, secondaryTop, secondaryBottom);
    
    // Add subtle wave distortion
    float waveEffect = waves(uv, iTime);
    color += vec3(waveEffect * 0.1, waveEffect * 0.15, waveEffect * 0.2);
    
    // Add very subtle noise for texture
    float noiseEffect = noise(uv * 3.0 + iTime * 0.05) * 0.03;
    color += vec3(noiseEffect);
    
    // Ensure colors stay in valid range
    color = clamp(color, 0.0, 1.0);
    
    fragColor = vec4(color, 1.0);
}

void main() {
    mainImage(gl_FragColor, gl_FragCoord.xy);
}
