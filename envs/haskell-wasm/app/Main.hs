{-# LANGUAGE DeriveGeneric #-}
module Main where

import Data.Aeson
import qualified Data.ByteString.Lazy.Char8 as BL
import GHC.Generics

newtype Ping = Ping { ping :: String } deriving (Generic)
newtype Pong = Pong { pong :: String } deriving (Generic)

instance FromJSON Ping
instance ToJSON Pong

main :: IO ()
main = do
    contents <- getLine
    let mPong = fmap (Pong . ping) (decode (BL.pack contents) :: Maybe Ping)
    case mPong of
        Just p  -> BL.putStrLn $ encode p
        Nothing -> putStrLn $ game contents

game contents = contents