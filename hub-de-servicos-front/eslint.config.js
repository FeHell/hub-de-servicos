import js from '@eslint/js'
import vue from 'eslint-plugin-vue'
import globals from 'globals'
import ts from 'typescript-eslint'
import prettier from 'eslint-config-prettier'

export default ts.config(
  { ignores: ['dist/**', 'coverage/**'] },
  js.configs.recommended,
  ...ts.configs.recommended,
  ...vue.configs['flat/recommended'],
  {
    files: ['src/**/*.{ts,vue}'],
    languageOptions: { globals: globals.browser, parserOptions: { parser: ts.parser } },
    rules: { 'vue/multi-word-component-names': 'off' },
  },
  {
    files: ['*.config.{js,ts}'],
    languageOptions: { globals: globals.node },
  },
  prettier,
)
