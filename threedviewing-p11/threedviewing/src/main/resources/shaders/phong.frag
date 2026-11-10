#version 330 core
out vec4 FragColor;

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;
in vec4 FragPosLightSpace;

uniform vec3 lightPos;
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform vec3 objectColor;

uniform sampler2D textureSampler;
uniform sampler2D shadowMap;

uniform bool useTexture;
uniform bool useBlinnPhong;
uniform bool useFlatShading;
uniform bool enableShadows;

float CalculationShadow(vec4 fragPosLightSpace, vec3 norm, vec3 lightDir) {
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    projCoords = projCoords * 0.5 + 0.5;
    
    if (projCoords.z > 1.0)
        return 0.0;
        
    float closestDepth = texture(shadowMap, projCoords.xy).r;
    float currentDepth = projCoords.z;
    
    // Dynamic Shadow Bias based on surface slope
    float bias = max(0.005 * (1.0 - dot(norm, lightDir)), 0.001);
    
    // PCF (Percentage-Closer Filtering) for soft shadows
    float shadow = 0.0;
    vec2 texelSize = 1.0 / textureSize(shadowMap, 0);
    for(int x = -1; x <= 1; ++x) {
        for(int y = -1; y <= 1; ++y) {
            float pcfDepth = texture(shadowMap, projCoords.xy + vec2(x, y) * texelSize).r;
            shadow += currentDepth - bias > pcfDepth ? 1.0 : 0.0;
        }
    }
    shadow /= 9.0;
    
    return shadow;
}

void main() {
    // Determine Normal (Flat Shading via dFdx/dFdy derivatives vs Smooth Vertex Normal)
    vec3 norm;
    if (useFlatShading) {
        vec3 fdx = dFdx(FragPos);
        vec3 fdy = dFdy(FragPos);
        norm = normalize(cross(fdx, fdy));
    } else {
        norm = normalize(Normal);
    }

    // 1. Ambient Lighting
    float ambientStrength = 0.25;
    vec3 ambient = ambientStrength * lightColor;

    // 2. Diffuse Lighting (Lambertian)
    vec3 lightDir = normalize(lightPos - FragPos);
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor;

    // 3. Specular Lighting (Phong vs Blinn-Phong)
    float specularStrength = 0.8;
    vec3 viewDir = normalize(viewPos - FragPos);
    float spec = 0.0;
    
    if (diff > 0.0) {
        if (useBlinnPhong) {
            vec3 halfwayDir = normalize(lightDir + viewDir);
            spec = pow(max(dot(norm, halfwayDir), 0.0), 32.0); // Blinn-Phong
        } else {
            vec3 reflectDir = reflect(-lightDir, norm);
            spec = pow(max(dot(viewDir, reflectDir), 0.0), 16.0); // Standard Phong
        }
    }
    vec3 specular = specularStrength * spec * lightColor;

    // 4. Calculate Shadow
    float shadow = enableShadows ? CalculationShadow(FragPosLightSpace, norm, lightDir) : 0.0;
    
    // Combine Base Color & Lighting Result
    vec3 baseColor = useTexture ? texture(textureSampler, TexCoords).rgb : objectColor;
    vec3 lighting = (ambient + (1.0 - shadow) * (diffuse + specular)) * baseColor;
    
    FragColor = vec4(lighting, 1.0);
}
