# Documents Schema

## User

- :xt/id
- :user/username
- :user/chat-id
- :user/first-name
- :user/last-name
- :user/language-code
- :user/point _UUID of current **Point** in **Blueprint** of user state or special symbol (**:main-menu**)_
- :user/instance _UUID of current **Instance** or **nil**_
- :user/variables _map of current state variables_

## Blueprint

- :xt/id
- :blueprint/title
- :blueprint/description
- :blueprint/root-point _UUID of root **Point**_

## Type

- :xt/id
- :type/blueprint _UUID of **Blueprint**_

## Point

- :xt/id
- :point/blueprint _UUID of **Blueprint**_
- :point/name
